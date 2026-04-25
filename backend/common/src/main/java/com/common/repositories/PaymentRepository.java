package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.PaymentEntity;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer>, JpaSpecificationExecutor<PaymentEntity> {
    // Tìm tất cả payments của một order
    List<PaymentEntity> findByOrderId(Integer orderId);
    
    // Tính tổng amount đã thanh toán cho một order
    // Quan trọng cho business rule: tổng Payment <= Order.totalAmount
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.orderId = :orderId")
    BigDecimal sumAmountByOrderId(@Param("orderId") Integer orderId);
    
    // Tìm payments theo cashier
    List<PaymentEntity> findByCashierId(Integer cashierId);
}
