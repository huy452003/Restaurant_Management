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
    long countByOrder_Id(Integer orderId);

    List<OrderItemEntity> findByOrder_IdIn(List<Integer> orderIds);

    @Query("""
        SELECT oi.order.id, COUNT(oi.id)
        FROM OrderItemEntity oi
        WHERE oi.order.id IN :orderIds
        GROUP BY oi.order.id
    """)
    List<Object[]> countByOrderIds(@Param("orderIds") List<Integer> orderIds);
}
