package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.OrderItemEntity;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Integer>, JpaSpecificationExecutor<OrderItemEntity> {
    boolean existsByOrder_Id(Integer orderId);
    int countByOrder_Id(Integer orderId);

    List<OrderItemEntity> findByOrder_IdIn(List<Integer> orderIds);

    // trả về list các order id và số lượng item của order đó
    @Query("""
        SELECT orderItem.order.id AS orderId, COUNT(orderItem.id) AS totalOrderItem
        FROM OrderItemEntity orderItem
        WHERE orderItem.order.id IN :orderIds
        GROUP BY orderItem.order.id
    """)
    List<Object[]> countByOrderIds(@Param("orderIds") List<Integer> orderIds);
}
