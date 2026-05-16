package com.app.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.OrderStatus;
import com.common.models.orderItem.OrderItemAdminUpdateModel;
import com.common.models.orderItem.OrderItemCreateModel;
import com.common.models.orderItem.OrderItemCustomerUpdateModel;
import com.common.models.orderItem.OrderItemModel;

public interface OrderItemService {
    Page<OrderItemModel> filters(
        Integer id, String orderNumber, OrderStatus orderItemStatus, Pageable pageable
    );
    List<OrderItemModel> create(List<OrderItemCreateModel> orderItems);
    OrderItemModel updateForCustomer(OrderItemCustomerUpdateModel update, Integer orderItemId);
    List<OrderItemModel> updateByAdmin(List<OrderItemAdminUpdateModel> updates, List<Integer> orderItemIds);
}
