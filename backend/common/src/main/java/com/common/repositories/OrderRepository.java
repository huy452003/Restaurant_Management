package com.common.repositories;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.OrderEntity;
import com.common.enums.OrderStatus;
import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer>, JpaSpecificationExecutor<OrderEntity> {
    // Tìm order theo orderNumber
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    
    // Tìm order cuối cùng trong ngày để generate số tiếp theo
    // Query: Tìm order có orderNumber bắt đầu bằng prefix, sắp xếp theo id DESC, lấy 1 record đầu tiên
    @Query("SELECT o FROM OrderEntity o WHERE o.orderNumber LIKE CONCAT(:prefix, '%') ORDER BY o.id DESC")
    Optional<OrderEntity> findLastOrderByPrefix(@Param("prefix") String prefix);
    
    // Tìm orders theo status
    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);
    
    // Tìm orders theo table
    List<OrderEntity> findByTable_TableNumber(Integer tableNumber);
    
    // Tìm orders theo waiter
    List<OrderEntity> findByWaiterId(Integer waiterId);
    
    // Kiểm tra xem order có tồn tại không
    boolean existsByOrderNumber(String orderNumber);

    /** Khóa order khi phiên thanh toán để tránh vượt tổng tiền khi có nhiều request đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> lockByIdForPayment(@Param("id") Integer id);
}
