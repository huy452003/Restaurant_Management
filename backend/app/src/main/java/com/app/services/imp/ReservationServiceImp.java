package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.services.ReservationService;
import com.app.services.TableStatusSyncService;
import com.app.utils.TableHoldGuard;
import com.app.utils.TableLookupUtils;
import com.app.utils.UserEntityUtils;
import com.common.utils.ReservationHoldUtils;
import com.common.repositories.OrderRepository;
import com.common.repositories.ReservationRepository;
import com.common.repositories.TableRepository;
import com.common.repositories.UserRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.utils.ReservationTimeSlots;
import com.common.entities.ReservationEntity;
import com.common.entities.TableEntity;
import com.common.enums.ReservationStatus;
import com.common.enums.TableStatus;
import com.common.enums.UserRole;
import com.common.models.reservation.ReservationAdminRequestModel;
import com.common.models.reservation.ReservationAvailabilityModel;
import com.common.models.reservation.ReservationCustomerCreateModel;
import com.common.models.reservation.ReservationCustomerUpdateModel;
import com.common.models.reservation.ReservationModel;
import com.common.enums.ReservationSlot;
import com.common.entities.UserEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.modelmapper.ModelMapper;

@Service
public class ReservationServiceImp implements ReservationService {
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private TableRepository tableRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserEntityUtils userEntityUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TableStatusSyncService tableStatusSyncService;

    private LogContext getLogContext(String methodName, List<Integer> reservationIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(reservationIds)
            .build();
    }

    private static final String RESERVATION_REDIS_KEY_PREFIX = "reservation:";
    private static final String TABLE_REDIS_KEY_PREFIX = "table:";

    @Override
    public Page<ReservationModel> filtersForCustomer(
        Integer id, Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus, Pageable pageable
    ) {
        return filtersInternal(
            id, null, null, null,
            tableNumber, reservationDate, reservationTime, numberOfGuests, reservationStatus, pageable,
            "filtersForCustomer"
        );
    }

    @Override
    public Page<ReservationModel> filtersForAdmin(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus, Pageable pageable
    ) {
        return filtersInternal(
            id, customerName, customerPhone, customerEmail,
            tableNumber, reservationDate, reservationTime, numberOfGuests, reservationStatus, pageable,
            "filtersForAdmin"
        );
    }

    private Page<ReservationModel> filtersInternal(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus, Pageable pageable, String methodName
    ) {
        LogContext logContext = getLogContext(methodName, Collections.emptyList());
        log.logInfo("Filtering reservations with pagination ...!", logContext);

        List<FilterCondition<ReservationEntity>> conditions = buildFilterConditions(
            id, customerName, customerPhone, customerEmail,
            tableNumber, reservationDate, reservationTime, numberOfGuests, reservationStatus
        );
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "ReservationModel", logContext, log
        );
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            conditions.add(FilterCondition.eq("customerEmail", currentUser.getEmail()));
        }

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            RESERVATION_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<ReservationModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, ReservationModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " reservations in cache", logContext);
            return cached;
        }

        Page<ReservationEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = reservationRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all reservations with pagination", logContext);
        }else {
            Specification<ReservationEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = reservationRepository.findAll(spec, pageable);
        }

        List<ReservationModel> pageDatas = pageEntities.getContent().stream().map(
            this::toReservationModel
        ).collect(Collectors.toList());

        Page<ReservationModel> reservationModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, reservationModelPage);
            log.logInfo("cached first-page filter snapshot for " + reservationModelPage.getTotalElements()
                + " reservations, key: " + redisKeyFilters, logContext);
        }
        return reservationModelPage;
    }

    @Override
    @Transactional
    public List<ReservationModel> create(List<ReservationCustomerCreateModel> reservations) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating reservations ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "ReservationModel", logContext, log
        );

        List<ReservationEntity> reservationEntities = reservations.stream().map(
            reservationModel -> {
                validateAndApplyTableBooking(
                    reservationModel.getTableNumber(),
                    reservationModel.getNumberOfGuests(),
                    reservationModel.getReservationDate(),
                    reservationModel.getReservationTime(),
                    null,
                    true,
                    logContext
                );

                ReservationEntity reservationEntity = new ReservationEntity();
                reservationEntity.setReservationDate(reservationModel.getReservationDate());
                reservationEntity.setReservationTime(reservationModel.getReservationTime());
                reservationEntity.setNumberOfGuests(reservationModel.getNumberOfGuests());
                reservationEntity.setSpecialRequest(reservationModel.getSpecialRequest());
                reservationEntity.setCustomerName(currentUser.getFullname());
                reservationEntity.setCustomerPhone(currentUser.getPhone());
                reservationEntity.setCustomerEmail(currentUser.getEmail());
                reservationEntity.setTable(TableLookupUtils.requireTable(
                    tableRepository, reservationModel.getTableNumber(), "ReservationModel", logContext, log
                ));
                reservationEntity.setReservationStatus(ReservationStatus.PENDING);
                return reservationEntity;
            }
        ).collect(Collectors.toList());

        reservationRepository.saveAll(reservationEntities);
        reservationEntities.stream()
            .map(r -> r.getTable().getTableNumber())
            .distinct()
            .forEach(tableNumber -> tableStatusSyncService.syncTableStatus(tableNumber));

        clearReservationAndTableCaches(logContext, "create");
        
        log.logInfo("completed, created " + reservationEntities.size() + " reservations", logContext);
        return reservationEntities.stream().map(
            this::toReservationModel
        ).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ReservationModel updateForCustomer(
        ReservationCustomerUpdateModel update, Integer reservationId
    ) {
        LogContext logContext = getLogContext(
            "updateForCustomer", Collections.singletonList(reservationId)
        );
        log.logInfo("Updating reservations ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "ReservationModel", logContext, log
        );
        ReservationEntity currentReservation = requireReservation(reservationId, logContext);

        if(!Objects.equals(currentReservation.getReservationStatus(), ReservationStatus.PENDING)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only update pending reservations",
                "ReservationModel",
                "reservation must be pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        if (currentUser.getRole() == UserRole.CUSTOMER) {
            if (!Objects.equals(currentReservation.getCustomerEmail(), currentUser.getEmail())) {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "You can only update your own reservations",
                    "ReservationModel",
                    "reservation owner must match authenticated customer"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
        }

        Integer tableNumber = currentReservation.getTable().getTableNumber();
        Boolean hasChanges = !Objects.equals(update.getReservationDate(), currentReservation.getReservationDate()) ||
                             !Objects.equals(update.getReservationTime(), currentReservation.getReservationTime()) ||
                             !Objects.equals(update.getNumberOfGuests(), currentReservation.getNumberOfGuests()) ||
                             !Objects.equals(update.getSpecialRequest(), currentReservation.getSpecialRequest());

        if(hasChanges) {
            applyTableTransition(
                currentReservation,
                tableNumber,
                update.getReservationDate(),
                update.getReservationTime(),
                currentReservation.getReservationStatus(),
                update.getNumberOfGuests(),
                logContext
            );
            currentReservation.setReservationDate(update.getReservationDate());
            currentReservation.setReservationTime(update.getReservationTime());
            currentReservation.setNumberOfGuests(update.getNumberOfGuests());
            currentReservation.setSpecialRequest(update.getSpecialRequest());
            reservationRepository.save(currentReservation);
            tableStatusSyncService.syncTableStatus(tableNumber);
            log.logInfo("completed, updated reservation with id: " + reservationId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        clearReservationAndTableCaches(logContext, "updateForCustomer");

        return toReservationModel(currentReservation);
    }

    @Override
    @Transactional
    public List<ReservationModel> updateByAdmin(
        List<ReservationAdminRequestModel> updates, List<Integer> reservationIds
    ) {
        LogContext logContext = getLogContext(
            "updateByAdmin", reservationIds != null ? reservationIds : Collections.emptyList()
        );
        log.logInfo("Updating reservations by admin/manager ...!", logContext);

        if (updates.size() != reservationIds.size()) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and reservationIds",
                reservationIds,
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<ReservationEntity> fetchedReservations = reservationRepository.findAllById(reservationIds);
        Map<Integer, ReservationEntity> reservationsById = fetchedReservations.stream()
            .collect(Collectors.toMap(ReservationEntity::getId, Function.identity()));
        List<ReservationEntity> foundReservations = reservationIds.stream().map(id -> {
            ReservationEntity reservation = reservationsById.get(id);
            if (reservation != null) {
                return reservation;
            }
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Reservation not found with id: " + id,
                Collections.singletonList(id),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }).collect(Collectors.toList());

        List<ReservationEntity> reservationsToUpdate = new ArrayList<>();
        Set<Integer> tablesToSync = new HashSet<>();
        Iterator<ReservationAdminRequestModel> reservationIterator = updates.iterator();
        Iterator<ReservationEntity> currentReservationIterator = foundReservations.iterator();

        while (reservationIterator.hasNext() && currentReservationIterator.hasNext()) {
            ReservationAdminRequestModel update = reservationIterator.next();
            ReservationEntity current = currentReservationIterator.next();
            Integer tableNumberBefore = current.getTable() != null
                ? current.getTable().getTableNumber() : null;

            Boolean hasChanges = !Objects.equals(update.getCustomerName(), current.getCustomerName()) ||
                                 !Objects.equals(update.getCustomerPhone(), current.getCustomerPhone()) ||
                                 !Objects.equals(update.getCustomerEmail(), current.getCustomerEmail()) ||
                                 !Objects.equals(update.getTableNumber(), current.getTable().getTableNumber()) ||
                                 !Objects.equals(update.getReservationDate(), current.getReservationDate()) ||
                                 !Objects.equals(update.getReservationTime(), current.getReservationTime()) ||
                                 !Objects.equals(update.getNumberOfGuests(), current.getNumberOfGuests()) ||
                                 !Objects.equals(update.getReservationStatus(), current.getReservationStatus()) ||
                                 !Objects.equals(update.getSpecialRequest(), current.getSpecialRequest());
            if (hasChanges) {
                UserEntity customerUser = userRepository.findByEmail(update.getCustomerEmail()).orElseThrow(() -> {
                    NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                        "User not found with email: " + update.getCustomerEmail(),
                        Collections.singletonList(update.getCustomerEmail()),
                        "ReservationModel"
                    );
                    log.logError(e.getMessage(), e, logContext);
                    return e;
                });
                applyTableTransition(
                    current,
                    update.getTableNumber(),
                    update.getReservationDate(),
                    update.getReservationTime(),
                    update.getReservationStatus(),
                    update.getNumberOfGuests(),
                    logContext
                );
                current.setReservationDate(update.getReservationDate());
                current.setReservationTime(update.getReservationTime());
                current.setNumberOfGuests(update.getNumberOfGuests());
                current.setReservationStatus(update.getReservationStatus());
                current.setSpecialRequest(update.getSpecialRequest());
                current.setCustomerName(customerUser.getFullname());
                current.setCustomerPhone(customerUser.getPhone());
                current.setCustomerEmail(customerUser.getEmail());
                current.setTable(TableLookupUtils.requireTable(
                    tableRepository, update.getTableNumber(), "ReservationModel", logContext, log
                ));
                if (tableNumberBefore != null) {
                    tablesToSync.add(tableNumberBefore);
                }
                if (current.getTable() != null) {
                    tablesToSync.add(current.getTable().getTableNumber());
                }
                reservationsToUpdate.add(current);
            }
        }

        if (!reservationsToUpdate.isEmpty()) {
            reservationRepository.saveAll(reservationsToUpdate);
            tablesToSync.forEach(tableStatusSyncService::syncTableStatus);
            log.logInfo("completed, updated " + reservationsToUpdate.size() + " reservations", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        clearReservationAndTableCaches(logContext, "updateByAdmin");

        return foundReservations.stream().map(
            this::toReservationModel
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationModel cancel(Integer reservationId) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(reservationId));
        log.logInfo("Cancelling reservation ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "ReservationModel", logContext, log
        );
        ReservationEntity foundReservation = requireReservation(reservationId, logContext);

        if(!Objects.equals(foundReservation.getReservationStatus(), ReservationStatus.CONFIRMED) &&
            !Objects.equals(foundReservation.getReservationStatus(), ReservationStatus.PENDING)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only cancel confirmed or pending reservations",
                "ReservationModel",
                "reservation must be confirmed or pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        if (currentUser.getRole() == UserRole.CUSTOMER) {
            if (!Objects.equals(foundReservation.getCustomerEmail(), currentUser.getEmail())) {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "You can only cancel your own reservations",
                    "ReservationModel",
                    "reservation owner must match authenticated customer"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
        }

        foundReservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(foundReservation);
        syncTableForReservation(foundReservation);

        clearReservationAndTableCaches(logContext, "cancel");
        log.logInfo("completed, cancelled reservation with id: " + reservationId, logContext);
        return toReservationModel(foundReservation);
    }

    @Override
    public ReservationAvailabilityModel getTimeSlotAvailability(Integer tableNumber, LocalDate date) {
        LogContext logContext = getLogContext("getTimeSlotAvailability", Collections.emptyList());
        
        userEntityUtils.requireAuthenticatedUser("ReservationModel", logContext, log);
        
        if (tableNumber == null || tableNumber < 1) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "tableNumber must be positive",
                Collections.singletonList(tableNumber),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (date == null) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "date must not be null",
                Collections.emptyList(),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        TableLookupUtils.requireTable(tableRepository, tableNumber, "ReservationModel", logContext, log);

        LocalDateTime now = LocalDateTime.now();
        List<String> bookedTimes = new ArrayList<>();
        for (ReservationSlot slot : ReservationSlot.all()) {
            LocalTime time = slot.toLocalTime();
            if (ReservationSlot.isPastSlot(date, time, now)) {
                continue;
            }
            if (reservationRepository.existsActiveSlot(
                tableNumber, date, time, ReservationHoldUtils.ACTIVE_HOLD_STATUSES, null
            )) {
                bookedTimes.add(slot.label());
            }
        }

        return new ReservationAvailabilityModel(tableNumber, date, bookedTimes);
    }

    // private method

    private void syncTableForReservation(ReservationEntity reservation) {
        if (reservation.getTable() != null) {
            tableStatusSyncService.syncTableStatus(reservation.getTable().getTableNumber());
        }
    }

    private void clearReservationAndTableCaches(LogContext logContext, String actionName) {
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, RESERVATION_REDIS_KEY_PREFIX);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
        log.logInfo("Deleted reservation and table filter caches after " + actionName, logContext);
    }

    private ReservationModel toReservationModel(ReservationEntity entity) {
        ReservationModel reservationModel = modelMapper.map(entity, ReservationModel.class);
        if(entity.getTable() != null) {
            reservationModel.setTableNumber(entity.getTable().getTableNumber());
        }
        return reservationModel;
    }

    private ReservationEntity requireReservation(Integer reservationId, LogContext logContext) {
        return reservationRepository.findById(reservationId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Reservation not found with id: " + reservationId,
                Collections.singletonList(reservationId),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    // Đồng bộ vòng đời trạng thái bàn khi reservation thay đổi (đổi bàn/hủy/chuyển terminal).
    private void applyTableTransition(
        ReservationEntity current, Integer newTableNumber,
        LocalDate newDate, LocalTime newTime, ReservationStatus requestedStatus, Integer numberOfGuests,
        LogContext logContext
    ) {
        ReservationStatus targetStatus = requestedStatus != null ? requestedStatus : current.getReservationStatus();
        LocalDate targetDate = newDate != null ? newDate : current.getReservationDate();
        LocalTime targetTime = newTime != null ? newTime : current.getReservationTime();
        boolean isTableChanged = !Objects.equals(newTableNumber, current.getTable().getTableNumber());

        if (ReservationHoldUtils.isTerminal(targetStatus)) {
            return;
        }

        validateAndApplyTableBooking(
            newTableNumber,
            numberOfGuests,
            targetDate,
            targetTime,
            current.getId(),
            isTableChanged,
            logContext
        );
    }

    // Validate nghiệp vụ đặt bàn (capacity, timeslot conflict, near-time reserve) và áp dụng trạng thái bàn.
    private void validateAndApplyTableBooking(
        Integer tableNumber,
        Integer numberOfGuests,
        LocalDate reservationDate,
        LocalTime reservationTime,
        Integer excludeReservationId,
        boolean requireTableAvailableInNearWindow,
        LogContext logContext
    ) {
        validateSchedule(reservationDate, reservationTime, logContext);
        validateCapacityForTable(tableNumber, numberOfGuests, logContext);
        TableHoldGuard.assertNoHoldingOrderOnTable(
            orderRepository, tableNumber, null, "ReservationModel", logContext, log
        );
        ensureSlotAvailable(tableNumber, reservationDate, reservationTime, excludeReservationId, logContext);

        if (!ReservationTimeSlots.isNearWindow(reservationDate, reservationTime)) {
            return;
        }

        if (requireTableAvailableInNearWindow) {
            TableLookupUtils.requireAvailableTable(
                tableRepository, tableNumber, "ReservationModel", logContext, log
            );
            return;
        }

        TableEntity table = TableLookupUtils.requireTable(
            tableRepository, tableNumber, "ReservationModel", logContext, log
        );
        TableStatus status = table.getTableStatus();
        if (status == TableStatus.AVAILABLE || status == TableStatus.RESERVED) {
            return;
        }

        ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
            "Table is not available for reservation in near-time window: " + tableNumber,
            "ReservationModel",
            "table must be available or reserved in near-time window"
        );
        log.logError(e.getMessage(), e, logContext);
        throw e;
    }

    // Kiểm tra sức chứa của bàn có đáp ứng số khách hay không.
    private void validateCapacityForTable(
        Integer tableNumber, Integer numberOfGuests, LogContext logContext
    ) {
        TableEntity table = TableLookupUtils.requireTable(
            tableRepository, tableNumber, "ReservationModel", logContext, log
        );
        if (numberOfGuests == null || numberOfGuests < 1) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "numberOfGuests must be at least 1",
                Collections.emptyList(),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (table.getCapacity() < numberOfGuests) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Table " + table.getTableNumber() + " holds up to " + table.getCapacity()
                    + " guests. Please reduce party size.",
                "ReservationModel",
                "table capacity must be enough"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private void validateSchedule(LocalDate reservationDate, LocalTime reservationTime, LogContext logContext) {
        if (reservationDate == null || reservationTime == null) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "reservationDate and reservationTime must not be null",
                Collections.emptyList(),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (!ReservationTimeSlots.isValidSlot(reservationTime)) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "reservationTime must be a 30-minute slot between 10:00 and 21:30",
                Collections.emptyList(),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (ReservationTimeSlots.isPastSlot(reservationDate, reservationTime, LocalDateTime.now())) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "reservation schedule must not be in the past",
                Collections.emptyList(),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private void ensureSlotAvailable(
        Integer tableNumber,
        LocalDate reservationDate,
        LocalTime reservationTime,
        Integer excludeReservationId,
        LogContext logContext
    ) {
        if (!reservationRepository.existsActiveSlot(
            tableNumber, reservationDate, reservationTime, ReservationHoldUtils.ACTIVE_HOLD_STATUSES, excludeReservationId
        )) {
            return;
        }
        ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
            "Table already has a reservation at "
                + ReservationTimeSlots.format(reservationTime) + " on " + reservationDate
                + " (table " + tableNumber + ")",
            "ReservationModel",
            "reservation slot must not conflict"
        );
        log.logError(e.getMessage(), e, logContext);
        throw e;
    }

    // Build danh sách điều kiện filter động cho query pagination.
    private List<FilterCondition<ReservationEntity>> buildFilterConditions(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus
    ) {
        List<FilterCondition<ReservationEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(StringUtils.hasText(customerName)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerName", customerName));
        }
        if(StringUtils.hasText(customerPhone)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerPhone", customerPhone));
        }
        if(StringUtils.hasText(customerEmail)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerEmail", customerEmail));
        }
        if(tableNumber != null) {
            conditions.add(FilterCondition.eq("table.tableNumber", tableNumber));
        }
        if(reservationDate != null) {
            conditions.add(FilterCondition.eq("reservationDate", reservationDate));
        }
        if(reservationTime != null) {
            conditions.add(FilterCondition.eq("reservationTime", reservationTime));
        }
        if(numberOfGuests != null) {
            conditions.add(FilterCondition.eq("numberOfGuests", numberOfGuests));
        }
        if(reservationStatus != null) {
            conditions.add(FilterCondition.eq("reservationStatus", reservationStatus));
        }
        return conditions;
    }
}
