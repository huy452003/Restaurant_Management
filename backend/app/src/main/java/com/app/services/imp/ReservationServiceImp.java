package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.services.ReservationService;
import com.app.utils.UserEntityUtils;
import com.common.repositories.ReservationRepository;
import com.common.repositories.TableRepository;
import com.common.repositories.UserRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.entities.ReservationEntity;
import com.common.entities.TableEntity;
import com.common.enums.ReservationStatus;
import com.common.enums.TableStatus;
import com.common.enums.UserRole;
import com.common.models.reservation.ReservationAdminRequestModel;
import com.common.models.reservation.ReservationCustomerRequestModel;
import com.common.models.reservation.ReservationModel;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final long NEAR_RESERVATION_WINDOW_MINUTES = 30L;
    private static final long RESERVATION_SLOT_DURATION_MINUTES = 90L;
    private static final List<ReservationStatus> ACTIVE_CONFLICT_STATUSES = Arrays.asList(
        ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED
    );

    @Override
    public Page<ReservationModel> filtersForCustomer(
        Integer id, Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus, Pageable pageable
    ) {
        return filtersInternal(
            id, null, null, null,
            tableNumber, reservationTs, numberOfGuests, reservationStatus, pageable,
            "filtersForCustomer"
        );
    }

    @Override
    public Page<ReservationModel> filtersForAdmin(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus, Pageable pageable
    ) {
        return filtersInternal(
            id, customerName, customerPhone, customerEmail,
            tableNumber, reservationTs, numberOfGuests, reservationStatus, pageable,
            "filtersForAdmin"
        );
    }

    private Page<ReservationModel> filtersInternal(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus, Pageable pageable, String methodName
    ) {
        LogContext logContext = getLogContext(methodName, Collections.emptyList());
        log.logInfo("Filtering reservations with pagination ...!", logContext);

        List<FilterCondition<ReservationEntity>> conditions = buildFilterConditions(
            id, customerName, customerPhone, customerEmail,
            tableNumber, reservationTs, numberOfGuests, reservationStatus
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
    public List<ReservationModel> create(List<ReservationCustomerRequestModel> reservations) {
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
                    reservationModel.getReservationTs(),
                    null,
                    true,
                    logContext
                );

                ReservationEntity reservationEntity = modelMapper.map(
                    reservationModel, ReservationEntity.class
                );
                reservationEntity.setCustomerName(currentUser.getFullname());
                reservationEntity.setCustomerPhone(currentUser.getPhone());
                reservationEntity.setCustomerEmail(currentUser.getEmail());
                reservationEntity.setTable(getTable(reservationModel.getTableNumber(), logContext));
                if (reservationEntity.getReservationStatus() == null) {
                    reservationEntity.setReservationStatus(ReservationStatus.PENDING);
                }
                return reservationEntity;
            }
        ).collect(Collectors.toList());

        reservationRepository.saveAll(reservationEntities);

        clearReservationAndTableCaches(logContext, "create");
        
        log.logInfo("completed, created " + reservationEntities.size() + " reservations", logContext);
        return reservationEntities.stream().map(
            this::toReservationModel
        ).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ReservationModel updateForCustomer(
        ReservationCustomerRequestModel update, Integer reservationId
    ) {
        LogContext logContext = getLogContext(
            "update", Collections.singletonList(reservationId)
        );
        log.logInfo("Updating reservations ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "ReservationModel", logContext, log
        );
        ReservationEntity currentReservation = reservationRepository.findById(reservationId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Reservation not found with id: " + reservationId,
                Collections.singletonList(reservationId),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

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

        Boolean hasChanges = !Objects.equals(update.getTableNumber(), currentReservation.getTable().getTableNumber()) ||
                             !Objects.equals(update.getReservationTs(), currentReservation.getReservationTs()) ||
                             !Objects.equals(update.getNumberOfGuests(), currentReservation.getNumberOfGuests()) ||
                             !Objects.equals(update.getSpecialRequest(), currentReservation.getSpecialRequest());

        if(hasChanges) {
            applyTableTransition(
                currentReservation,
                update.getTableNumber(),
                update.getReservationTs(),
                currentReservation.getReservationStatus(),
                update.getNumberOfGuests(),
                logContext
            );
            modelMapper.map(update, currentReservation);
            currentReservation.setTable(getTable(update.getTableNumber(), logContext));
            reservationRepository.save(currentReservation);
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
        Iterator<ReservationAdminRequestModel> reservationIterator = updates.iterator();
        Iterator<ReservationEntity> currentReservationIterator = foundReservations.iterator();

        while (reservationIterator.hasNext() && currentReservationIterator.hasNext()) {
            ReservationAdminRequestModel update = reservationIterator.next();
            ReservationEntity current = currentReservationIterator.next();

            Boolean hasChanges = !Objects.equals(update.getCustomerName(), current.getCustomerName()) ||
                                 !Objects.equals(update.getCustomerPhone(), current.getCustomerPhone()) ||
                                 !Objects.equals(update.getCustomerEmail(), current.getCustomerEmail()) ||
                                 !Objects.equals(update.getTableNumber(), current.getTable().getTableNumber()) ||
                                 !Objects.equals(update.getReservationTs(), current.getReservationTs()) ||
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
                    update.getReservationTs(),
                    update.getReservationStatus(),
                    update.getNumberOfGuests(),
                    logContext
                );
                modelMapper.map(update, current);
                current.setCustomerName(customerUser.getFullname());
                current.setCustomerPhone(customerUser.getPhone());
                current.setCustomerEmail(customerUser.getEmail());
                current.setTable(getTable(update.getTableNumber(), logContext));
                reservationsToUpdate.add(current);
            }
        }

        if (!reservationsToUpdate.isEmpty()) {
            reservationRepository.saveAll(reservationsToUpdate);
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
        ReservationEntity foundReservation = reservationRepository.findById(reservationId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Reservation not found with id: " + reservationId,
                Collections.singletonList(reservationId),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

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

        applyTableTransition(
            foundReservation,
            foundReservation.getTable().getTableNumber(),
            foundReservation.getReservationTs(),
            ReservationStatus.CANCELLED,
            foundReservation.getNumberOfGuests(),
            logContext
        );
        foundReservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(foundReservation);
        
        clearReservationAndTableCaches(logContext, "cancel");
        log.logInfo("completed, cancelled reservation with id: " + reservationId, logContext);
        return toReservationModel(foundReservation);
    }

    // private method

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

    private TableEntity getTable(Integer tableNumber, LogContext logContext) {
        return tableRepository.findByTableNumber(tableNumber).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Table not found with tableNumber: " + tableNumber,
                Collections.singletonList(tableNumber),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    // Lấy bàn chỉ khi đang AVAILABLE để đảm bảo có thể reserve ngay.
    private TableEntity getTableWithAvailableStatus(Integer tableNumber, LogContext logContext) {
        return tableRepository.findByTableNumberAndTableStatus(
            tableNumber, TableStatus.AVAILABLE).orElseThrow(() -> {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Table is not available: " + tableNumber + ", please choose another table",
                "ReservationModel",
                "table must be available"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    // Đồng bộ vòng đời trạng thái bàn khi reservation thay đổi (đổi bàn/hủy/chuyển terminal).
    private void applyTableTransition(
        ReservationEntity current, Integer newTableNumber,
        LocalDateTime newReservationTs, ReservationStatus requestedStatus, Integer numberOfGuests,
        LogContext logContext
    ) {
        ReservationStatus targetStatus = requestedStatus != null ? requestedStatus : current.getReservationStatus();
        LocalDateTime targetReservationTs = newReservationTs != null ? newReservationTs : current.getReservationTs();
        boolean isTableChanged = !Objects.equals(newTableNumber, current.getTable().getTableNumber());

        if (isTerminalStatus(targetStatus)) {
            releaseTableIfNoOtherNearActiveReservation(current);
            return;
        }

        validateAndApplyTableBooking(
            newTableNumber,
            numberOfGuests,
            targetReservationTs,
            current.getId(),
            isTableChanged,
            logContext
        );

        if (isTableChanged) {
            releaseTableIfNoOtherNearActiveReservation(current);
        }
    }

    // Validate nghiệp vụ đặt bàn (capacity, timeslot conflict, near-time reserve) và áp dụng trạng thái bàn.
    private void validateAndApplyTableBooking(
        Integer tableNumber,
        Integer numberOfGuests,
        LocalDateTime reservationTs,
        Integer excludeReservationId,
        boolean requireTableAvailableInNearWindow,
        LogContext logContext
    ) {
        if (reservationTs == null) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "reservationTs must not be null",
                Collections.singletonList(tableNumber),
                "ReservationModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        validateCapacityForTable(tableNumber, numberOfGuests, logContext);
        ensureNoActiveTimeslotConflict(tableNumber, reservationTs, excludeReservationId, logContext);

        if (!isNearReservationWindow(reservationTs)) {
            return;
        }

        if (requireTableAvailableInNearWindow) {
            TableEntity table = getTableWithAvailableStatus(tableNumber, logContext);
            table.setTableStatus(TableStatus.RESERVED);
            tableRepository.save(table);
            return;
        }

        TableEntity table = getTable(tableNumber, logContext);
        if (table.getTableStatus() == TableStatus.AVAILABLE) {
            table.setTableStatus(TableStatus.RESERVED);
            tableRepository.save(table);
            return;
        }
        if (table.getTableStatus() == TableStatus.RESERVED) {
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
        TableEntity table = getTable(tableNumber, logContext);
        if (table.getCapacity() < numberOfGuests) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Table capacity is not enough: " + table.getTableNumber() +
                ", please try again with a smaller number of guests: " + table.getCapacity(),
                "ReservationModel",
                "table capacity must be enough"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    // Chặn double-booking: kiểm tra bàn có bị trùng khung giờ với reservation active khác không.
    private void ensureNoActiveTimeslotConflict(
        Integer tableNumber, LocalDateTime reservationTs, Integer excludeReservationId, LogContext logContext
    ) {
        LocalDateTime slotStart = reservationTs.minusMinutes(RESERVATION_SLOT_DURATION_MINUTES);
        LocalDateTime slotEnd = reservationTs.plusMinutes(RESERVATION_SLOT_DURATION_MINUTES);
        boolean hasConflict = reservationRepository.existsActiveTimeslotConflict(
            tableNumber, slotStart, slotEnd, ACTIVE_CONFLICT_STATUSES, excludeReservationId
        );
        if (!hasConflict) {
            return;
        }
        ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
            "Table already has an active reservation in this timeslot: " + tableNumber,
            "ReservationModel",
            "reservation timeslot must not conflict"
        );
        log.logError(e.getMessage(), e, logContext);
        throw e;
    }

    // Xác định reservation đã nằm trong cửa sổ near-time để khóa bàn vận hành chưa.
    private boolean isNearReservationWindow(LocalDateTime reservationTs) {
        return !reservationTs.isAfter(LocalDateTime.now().plusMinutes(NEAR_RESERVATION_WINDOW_MINUTES));
    }

    // Đánh dấu các trạng thái kết thúc để trả bàn về AVAILABLE.
    private boolean isTerminalStatus(ReservationStatus status) {
        return status == ReservationStatus.CANCELLED ||
            status == ReservationStatus.COMPLETED ||
            status == ReservationStatus.NO_SHOW;
    }

    // Trả bàn về AVAILABLE khi reservation kết thúc hoặc đổi sang bàn khác.
    private void releaseTableIfNoOtherNearActiveReservation(ReservationEntity reservation) {
        TableEntity table = reservation.getTable();
        if (table == null || table.getTableStatus() == TableStatus.AVAILABLE) {
            return;
        }
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(NEAR_RESERVATION_WINDOW_MINUTES);
        LocalDateTime windowEnd = LocalDateTime.now().plusMinutes(NEAR_RESERVATION_WINDOW_MINUTES);
        boolean hasOtherActiveNearReservation = reservationRepository.existsActiveReservationInWindow(
            table.getTableNumber(),
            windowStart,
            windowEnd,
            ACTIVE_CONFLICT_STATUSES,
            reservation.getId()
        );
        if (hasOtherActiveNearReservation) {
            return;
        }
        if (table.getTableStatus() == TableStatus.AVAILABLE) {
            return;
        }
        table.setTableStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }

    // Build danh sách điều kiện filter động cho query pagination.
    private List<FilterCondition<ReservationEntity>> buildFilterConditions(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus
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
        if(reservationTs != null) {
            conditions.add(FilterCondition.eq("reservationTs", reservationTs));
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
