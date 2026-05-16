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
import com.common.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer>, JpaSpecificationExecutor<OrderEntity> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> lockByIdForPayment(@Param("id") Integer id);

    boolean existsByTable_TableNumberAndOrderStatus(Integer tableNumber, OrderStatus orderStatus);

    boolean existsByTable_TableNumberAndOrderStatusIn(
        Integer tableNumber, List<OrderStatus> orderStatuses
    );

    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM OrderEntity o
        WHERE o.table.tableNumber = :tableNumber
          AND o.orderStatus IN :statuses
          AND (:excludeOrderId IS NULL OR o.id <> :excludeOrderId)
        """)
    boolean existsActiveHoldingOrderOnTable(
        @Param("tableNumber") Integer tableNumber,
        @Param("statuses") List<OrderStatus> statuses,
        @Param("excludeOrderId") Integer excludeOrderId
    );

    @Query("""
        SELECT DISTINCT o.table.tableNumber FROM OrderEntity o
        WHERE o.orderStatus IN :statuses AND o.table IS NOT NULL
        """)
    List<Integer> findDistinctTableNumbersByOrderStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM OrderEntity o WHERE o.orderStatus = :status AND o.createdAt < :cutoff")
    List<OrderEntity> findByOrderStatusAndCreatedAtBefore(
        @Param("status") OrderStatus status,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT DISTINCT o.table.tableNumber FROM OrderEntity o WHERE o.orderStatus = :status AND o.table IS NOT NULL")
    List<Integer> findDistinctTableNumbersByOrderStatus(@Param("status") OrderStatus status);

    /** CONFIRMED, chưa có payment nào, quá hạn kể từ updatedAt (sau xác nhận đơn). */
    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.orderStatus = :orderStatus
          AND o.updatedAt < :cutoff
          AND NOT EXISTS (SELECT 1 FROM PaymentEntity p WHERE p.orderId = o.id)
        """)
    List<OrderEntity> findConfirmedWithoutPaymentsOlderThan(
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("cutoff") LocalDateTime cutoff
    );

    /** CONFIRMED, chỉ payment FAILED/CANCELLED (không PENDING/COMPLETED), lần thử cuối quá hạn. */
    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.orderStatus = :orderStatus
          AND NOT EXISTS (
            SELECT 1 FROM PaymentEntity p
            WHERE p.orderId = o.id AND p.paymentStatus IN :blockingStatuses
          )
          AND EXISTS (SELECT 1 FROM PaymentEntity p WHERE p.orderId = o.id)
          AND (
            SELECT MAX(COALESCE(p.paidAt, p.updatedAt))
            FROM PaymentEntity p WHERE p.orderId = o.id
          ) < :cutoff
        """)
    List<OrderEntity> findConfirmedWithOnlyTerminalPaymentsOlderThan(
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("blockingStatuses") Collection<PaymentStatus> blockingStatuses,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Query("""
        SELECT DISTINCT o FROM OrderEntity o
        INNER JOIN PaymentEntity p ON p.orderId = o.id
        WHERE o.orderStatus = :orderStatus
          AND p.paymentStatus = :pendingStatus
          AND p.createdAt < :cutoff
          AND NOT EXISTS (
            SELECT 1 FROM PaymentEntity p2
            WHERE p2.orderId = o.id AND p2.paymentStatus = :completedStatus
          )
        """)
    List<OrderEntity> findWithStalePendingPaymentAndNoCompleted(
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("pendingStatus") PaymentStatus pendingStatus,
        @Param("completedStatus") PaymentStatus completedStatus,
        @Param("cutoff") LocalDateTime cutoff
    );
}
