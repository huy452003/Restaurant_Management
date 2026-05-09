package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.services.ShiftService;
import com.app.utils.UserEntityUtils;
import com.common.repositories.ShiftRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.models.user.ShiftModel;
import com.common.entities.ShiftEntity;
import com.common.entities.UserEntity;
import com.common.enums.ShiftStatus;
import com.common.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Function;

import org.modelmapper.ModelMapper;

@Service
public class ShiftServiceImp implements ShiftService {
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private UserEntityUtils userEntityUtils;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

    private LogContext getLogContext(String methodName, List<Integer> shiftIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(shiftIds)
            .build();
    }

    private static final String SHIFT_REDIS_KEY_PREFIX = "shift:";

    @Override
    public Page<ShiftModel> filters(
        Integer id, LocalDate shiftDate, 
        LocalDateTime startTime, LocalDateTime endTime,
        ShiftStatus shiftStatus, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering shifts with pagination ...!", logContext);

        List<FilterCondition<ShiftEntity>> conditions = buildFilterConditions(
            id, shiftDate, startTime, endTime, shiftStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            SHIFT_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<ShiftModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, ShiftModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " shifts in cache", logContext);
            return cached;
        }

        Page<ShiftEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = shiftRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all shifts with pagination", logContext);
        }else {
            Specification<ShiftEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = shiftRepository.findAll(spec, pageable);
        }

        List<ShiftModel> pageDatas = pageEntities.getContent().stream().map(
            this::toShiftModel
        ).collect(Collectors.toList());

        Page<ShiftModel> shiftModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, shiftModelPage);
            log.logInfo("cached first-page filter snapshot for " + shiftModelPage.getTotalElements()
                + " shifts, key: " + redisKeyFilters, logContext);
        }
        return shiftModelPage;
    }

    @Override
    public List<ShiftModel> create(List<ShiftModel> shifts) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating shifts ...!", logContext);

        Map<Integer, UserEntity> employeesById = shifts.stream()
            .map(ShiftModel::getEmployeeId)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(),
                employeeId -> userEntityUtils.requireById(employeeId, "UserModel", logContext, log)
            ));

        validateRole(shifts, logContext);

        List<Object> conflicts = new ArrayList<>();
        for (ShiftModel shift : shifts) {
            if (hasOverlappingShift(shift, null)) {
                conflicts.add(buildShiftOverlapConflict(shift, null));
            }
        }
        if (!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Shift schedule conflicts detected",
                conflicts,
                "ShiftModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<ShiftEntity> shiftEntities = shifts.stream().map(
            shiftModel -> {
                ShiftEntity shiftEntity = modelMapper.map(shiftModel, ShiftEntity.class);
                shiftEntity.setEmployee(employeesById.get(shiftModel.getEmployeeId()));
                return shiftEntity;
            }
        ).collect(Collectors.toList());

        shiftRepository.saveAll(shiftEntities);

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, SHIFT_REDIS_KEY_PREFIX);
        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created " + shiftEntities.size() + " shifts", logContext);
        return shiftEntities.stream().map(
            this::toShiftModel
        ).collect(Collectors.toList());
    }
    
    @Override
    public List<ShiftModel> update(List<ShiftModel> updates, List<Integer> shiftIds) {
        LogContext logContext = getLogContext(
            "update", shiftIds != null ? shiftIds : Collections.emptyList()
        );
        log.logInfo("Updating shifts ...!", logContext);

        if(updates.size() != shiftIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and shiftIds",
                shiftIds,
                "ShiftModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        validateRole(updates, logContext);

        List<Object> conflicts = new ArrayList<>();
        for (int i = 0; i < updates.size(); i++) {
            ShiftModel shift = updates.get(i);
            Integer excludeId = shiftIds.get(i);
            if (hasOverlappingShift(shift, excludeId)) {
                conflicts.add(buildShiftOverlapConflict(shift, excludeId));
            }
        }
        if (!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Shift schedule conflicts detected",
                conflicts,
                "ShiftModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<ShiftEntity> foundShifts = shiftIds.stream().map(
            id -> shiftRepository.findById(id).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "Shift not found with id: " + id,
                    Collections.singletonList(id),
                    "ShiftModel"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            })
        ).collect(Collectors.toList());
        log.logInfo("found " + foundShifts.size() + " shifts", logContext);

        List<ShiftEntity> shiftsToUpdate = new ArrayList<>();
        Iterator<ShiftModel> shiftIterator = updates.iterator();
        Iterator<ShiftEntity> currentShiftIterator = foundShifts.iterator();

        while(shiftIterator.hasNext() && currentShiftIterator.hasNext()) {
            ShiftModel update = shiftIterator.next();
            ShiftEntity current = currentShiftIterator.next();

            Boolean hasChanges = !Objects.equals(update.getEmployeeId(), current.getEmployee().getId()) ||
                                 !Objects.equals(update.getShiftDate(), current.getShiftDate()) ||
                                 !Objects.equals(update.getStartTime(), current.getStartTime()) ||
                                 !Objects.equals(update.getEndTime(), current.getEndTime()) ||
                                 !Objects.equals(update.getShiftStatus(), current.getShiftStatus()) ||
                                 !Objects.equals(update.getNotes(), current.getNotes());
            if(hasChanges) {
                modelMapper.map(update, current);
                current.setEmployee(
                    userEntityUtils.requireById(
                        update.getEmployeeId(), "UserModel", logContext, log)
                    );
                shiftsToUpdate.add(current);
            }
        }

        if(!shiftsToUpdate.isEmpty()) {
            shiftRepository.saveAll(shiftsToUpdate);
            log.logInfo("completed, updated " + shiftsToUpdate.size() + " shifts", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, SHIFT_REDIS_KEY_PREFIX);

        return foundShifts.stream().map(
            this::toShiftModel
        ).collect(Collectors.toList());
    }

    // private method

    private ShiftModel toShiftModel(ShiftEntity entity) {
        ShiftModel shiftModel = modelMapper.map(entity, ShiftModel.class);
        shiftModel.setTotalWorkingHours(
            calculateTotalWorkingHours(entity.getStartTime(), entity.getEndTime())
        );
        if(entity.getEmployee() != null) {
            shiftModel.setEmployeeId(entity.getEmployee().getId());
        }
        return shiftModel;
    }

    

    private void validateRole(List<ShiftModel> shifts, LogContext logContext) {
        List<Object> invalidFields = new ArrayList<>();
        for(ShiftModel shift : shifts) {
            UserEntity employee = userEntityUtils.requireById(
                shift.getEmployeeId(), "UserModel", logContext, log
            );
            if(!Objects.equals(employee.getRole(), UserRole.MANAGER) &&
               !Objects.equals(employee.getRole(), UserRole.WAITER) &&
               !Objects.equals(employee.getRole(), UserRole.CHEF) &&
               !Objects.equals(employee.getRole(), UserRole.CASHIER)
            ) {
                Map<String, Object> invalidField = new HashMap<>();
                invalidField.put("employeeId", shift.getEmployeeId());
                invalidField.put("field", "role");
                invalidField.put("value", employee.getRole());
                invalidField.put("message", "Employee role must be MANAGER, WAITER, CHEF or CASHIER");
                invalidFields.add(invalidField);
            }
            invalidFields.addAll(validateShiftTimeFields(shift));
            invalidFields.addAll(validateCreateShiftScheduledForward(shift));
        }
        if(!invalidFields.isEmpty()) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Invalid fields detected",
                invalidFields,
                "ShiftModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    // tính tổng số giờ làm việc
    private Integer calculateTotalWorkingHours(LocalDateTime startTime, LocalDateTime endTime) {
        return (int) Duration.between(startTime, endTime).toHours();
    }

    // validate các field time của shift
    private List<Object> validateShiftTimeFields(ShiftModel shift) {
        List<Object> invalidFields = new ArrayList<>();

        // giờ kết thúc phải sau giờ bắt đầu
        if(!shift.getEndTime().isAfter(shift.getStartTime())) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "endTime");
            invalidField.put("value", shift.getEndTime());
            invalidField.put("message", "End time must be after start time");
            invalidFields.add(invalidField);
        }

        // ngày làm việc phải trùng với ngày bắt đầu ca làm việc
        if(!Objects.equals(shift.getShiftDate(), shift.getStartTime().toLocalDate())) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "startTime");
            invalidField.put("value", shift.getStartTime());
            invalidField.put("message", "Start time must match shift date");
            invalidFields.add(invalidField);
        }

        // ngày làm việc phải trùng với ngày kết thúc ca làm việc
        if(!Objects.equals(shift.getShiftDate(), shift.getEndTime().toLocalDate())) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "endTime");
            invalidField.put("value", shift.getEndTime());
            invalidField.put("message", "End time must match shift date");
            invalidFields.add(invalidField);
        }

        // tổng số giờ làm việc phải nhỏ hơn hoặc bằng 8 giờ
        int totalWorkingHours = calculateTotalWorkingHours(shift.getStartTime(), shift.getEndTime());
        if(totalWorkingHours > 8) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "totalWorkingHours");
            invalidField.put("value", totalWorkingHours);
            invalidField.put("message", "Total working hours must be less than or equal to 8 hours");
            invalidFields.add(invalidField);
        }

        return invalidFields;
    }

    // validate ngày ca không trong quá khứ; end sau hiện tại
    private List<Object> validateCreateShiftScheduledForward(ShiftModel shift) {
        List<Object> invalidFields = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (shift.getShiftDate().isBefore(today)) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "shiftDate");
            invalidField.put("value", shift.getShiftDate());
            invalidField.put("message", "Shift date must be today or a future date");
            invalidFields.add(invalidField);
        }
        if (!shift.getEndTime().isAfter(LocalDateTime.now())) {
            Map<String, Object> invalidField = new HashMap<>();
            invalidField.put("employeeId", shift.getEmployeeId());
            invalidField.put("field", "endTime");
            invalidField.put("value", shift.getEndTime());
            invalidField.put("message", "When creating a shift, end time must be after the current time");
            invalidFields.add(invalidField);
        }
        return invalidFields;
    }

    // validate xung đột ca làm việc
    private boolean hasOverlappingShift(ShiftModel shift, Integer excludeId) {
        return shiftRepository.existsOverlappingShift(
            shift.getEmployeeId(),
            shift.getStartTime(),
            shift.getEndTime(),
            excludeId,
            ShiftStatus.CANCELLED
        );
    }

    // build conflict khi có xung đột ca làm việc
    private Map<String, Object> buildShiftOverlapConflict(ShiftModel shift, Integer excludeId) {
        Map<String, Object> conflict = new HashMap<>();
        conflict.put("employeeId", shift.getEmployeeId());
        conflict.put("excludeId", excludeId);
        conflict.put("startTime", shift.getStartTime());
        conflict.put("endTime", shift.getEndTime());
        conflict.put("message", "Shift overlaps with an existing shift for this employee");
        return conflict;
    }

    private List<FilterCondition<ShiftEntity>> buildFilterConditions(
        Integer id, LocalDate shiftDate, 
        LocalDateTime startTime, LocalDateTime endTime,
        ShiftStatus shiftStatus
    ) {
        List<FilterCondition<ShiftEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(shiftDate != null) {
            conditions.add(FilterCondition.eq("shiftDate", shiftDate));
        }
        if(startTime != null) {
            conditions.add(FilterCondition.eq("startTime", startTime));
        }
        if(endTime != null) {
            conditions.add(FilterCondition.eq("endTime", endTime));
        }
        if(shiftStatus != null) {
            conditions.add(FilterCondition.eq("shiftStatus", shiftStatus));
        }
        return conditions;
    }
}
