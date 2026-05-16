package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.PaymentEntity;
import com.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.common.enums.PaymentMethod;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer>, JpaSpecificationExecutor<PaymentEntity> {

    List<PaymentEntity> findByOrder_IdAndPaymentStatus(Integer orderId, PaymentStatus paymentStatus);

    Optional<PaymentEntity> findFirstByOrder_IdAndPaymentMethodAndPaymentStatus(
        Integer orderId, PaymentMethod paymentMethod, PaymentStatus paymentStatus
    );
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.orderId = :orderId AND p.paymentStatus IN (:statuses)")
    BigDecimal sumAmountByOrderIdAndPaymentStatuses(@Param("orderId") Integer orderId, @Param("statuses") Collection<PaymentStatus> statuses);

    @Query(
        "SELECT p.orderId, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p "
            + "WHERE p.orderId IN :orderIds AND p.paymentStatus IN (:statuses) GROUP BY p.orderId"
    )
    List<Object[]> sumAllocatedAmountsByOrderIds(
        @Param("orderIds") Collection<Integer> orderIds,
        @Param("statuses") Collection<PaymentStatus> statuses
    );

    default BigDecimal sumCompletedAmountByOrderId(Integer orderId) {
        return sumAmountByOrderIdAndPaymentStatuses(orderId, List.of(PaymentStatus.COMPLETED));
    }

    boolean existsByOrderIdAndPaymentStatus(Integer orderId, PaymentStatus paymentStatus);

    @Query(
        "SELECT DISTINCT p.orderId FROM PaymentEntity p "
            + "WHERE p.orderId IN :orderIds AND p.paymentStatus = :status"
    )
    List<Integer> findDistinctOrderIdsByOrderIdInAndPaymentStatus(
        @Param("orderIds") Collection<Integer> orderIds,
        @Param("status") PaymentStatus status
    );
}
