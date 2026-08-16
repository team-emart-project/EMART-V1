package com.example.demo.mapper;

import com.example.demo.dto.response.OrderDetailResponse;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.Orders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entity -> DTO for orders.
 *
 * Reads only the snapshot columns on order_details, never the live product
 * row. An invoice must not change because someone edited the catalogue.
 */
@Component
public class OrderMapper {

    private final AddressMapper addressMapper;

    public OrderMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public OrderDetailResponse toItemResponse(OrderDetail item) {
        BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
        BigDecimal lineTotal = item.getPriceCharged().multiply(qty);
        BigDecimal lineAtMrp = item.getMrpPrice().multiply(qty);

        return OrderDetailResponse.builder()
                .orderDtlId(item.getOrderDtlId())
                .prodId(item.getProduct() != null ? item.getProduct().getProdId() : null)
                .prodName(item.getProdNameSnapshot())
                .quantity(item.getQuantity())
                .mrpPrice(item.getMrpPrice())
                .cardholderPrice(item.getCardholderPrice())
                .priceOption(item.getPriceOption())
                .priceCharged(item.getPriceCharged())
                .lineTotal(lineTotal)
                .lineSavings(lineAtMrp.subtract(lineTotal))
                .pointsRedeemed(item.getPointsRedeemed())
                .build();
    }

    /**
     * @param pointsBalanceAfter only known after a successful payment; null elsewhere.
     */
    public OrderResponse toResponse(Orders order, Integer pointsBalanceAfter) {

        List<OrderDetailResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotalMrp = items.stream()
                .map(i -> i.getMrpPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .orderDate(order.getOrderDate())
                .customerName(order.getUser().getFirstName() + " "
                        + (order.getUser().getLastName() == null ? "" : order.getUser().getLastName()))
                .membershipNo(order.getUser().getMembershipNo())
                .cardholder(order.getUser().getIsCardholder())
                .shippingAddress(addressMapper.toResponse(order.getShippingAddress()))
                .billingAddress(addressMapper.toResponse(order.getBillingAddress()))
                .items(items)
                .subtotalMrp(subtotalMrp)
                .subtotalAmount(order.getSubtotalAmount())
                .totalSavings(subtotalMrp.subtract(order.getSubtotalAmount()))
                .totalAmount(order.getTotalAmount())
                .pointsRedeemed(order.getPointsRedeemed())
                .pointsEarned(order.getPointsEarned())
                .pointsBalanceAfter(pointsBalanceAfter)
                .paymentStatus(order.getPaymentStatus().name())
                .orderStatus(order.getOrderStatus().name())
                .build();
    }
}
