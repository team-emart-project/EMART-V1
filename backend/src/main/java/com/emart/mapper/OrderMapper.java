package com.emart.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.emart.dto.response.OrderDetailResponse;
import com.emart.dto.response.OrderResponse;
import com.emart.entity.OrderDetail;
import com.emart.entity.Orders;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Orders order, Boolean preview) {

        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getOrderId());
        response.setOrderNo(order.getOrderNo());
        response.setOrderDate(order.getOrderDate());

        response.setSubtotal(order.getSubtotalAmount());
        response.setTotal(order.getTotalAmount());

        response.setPointsEarned(order.getPointsEarned());
        response.setPointsRedeemed(order.getPointsRedeemed());

        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setOrderStatus(order.getOrderStatus().name());

        response.setPreview(preview);

        List<OrderDetailResponse> items =
                order.getItems()
                     .stream()
                     .map(this::toDetailResponse)
                     .collect(Collectors.toList());

        response.setItems(items);

        return response;
    }

    public OrderDetailResponse toDetailResponse(OrderDetail detail) {

        OrderDetailResponse response = new OrderDetailResponse();

        response.setProductId(detail.getProduct().getProdId());

        response.setProductName(detail.getProdNameSnapshot());

        response.setQuantity(detail.getQuantity());

        response.setMrpPrice(detail.getMrpPrice());

        response.setCardholderPrice(detail.getCardholderPrice());

        response.setPriceOption(detail.getPriceOption());

        response.setPriceCharged(detail.getPriceCharged());

        response.setPointsRedeemed(detail.getPointsRedeemed());

        return response;
    }

}