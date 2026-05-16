package com.app.services.imp;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.app.services.OrderItemStatusSyncService;
import com.app.utils.OrderItemStatusSyncUtils;
import com.common.entities.OrderItemEntity;
import com.common.enums.OrderStatus;
import com.common.repositories.OrderItemRepository;
import com.common.utils.FilterPageCacheFacade;

@Service
public class OrderItemStatusSyncServiceImp implements OrderItemStatusSyncService {

    private static final String ORDER_ITEM_REDIS_KEY_PREFIX = "order-item:";

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void syncItemsWithOrderStatus(
        Integer orderId,
        OrderStatus newStatus,
        OrderStatus previousStatus
    ) {
        if (orderId == null || newStatus == null || Objects.equals(newStatus, previousStatus)) {
            return;
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrder_IdIn(
            Collections.singletonList(orderId)
        );
        List<OrderItemEntity> changed = OrderItemStatusSyncUtils.syncItemsWithOrderStatus(items, newStatus);
        if (!changed.isEmpty()) {
            orderItemRepository.saveAll(changed);
            FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_ITEM_REDIS_KEY_PREFIX);
        }
    }
}
