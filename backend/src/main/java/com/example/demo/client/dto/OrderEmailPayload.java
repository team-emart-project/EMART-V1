package com.example.demo.client.dto;

import com.example.demo.dto.response.AddressResponse;
import com.example.demo.dto.response.OrderDetailResponse;
import com.example.demo.dto.response.OrderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The request body of POST /api/send-order-email on backend-email-microservice.
 *
 * WHY A SEPARATE TYPE INSTEAD OF POSTING OrderResponse DIRECTLY — OrderResponse
 * is what this backend promises the FRONTEND. Posting it to another service
 * would quietly make it two contracts at once, and the next time the UI needs a
 * field renamed, an email in production breaks. This class is the contract with
 * the email service and nothing else; when it changes, only the email service
 * has to agree.
 *
 * Records, so the payload is built once and cannot be edited on its way to the
 * wire. Field names match the microservice's DTOs exactly — that is the whole
 * contract, so do not rename anything here without changing it there too.
 */
public record OrderEmailPayload(String sourceSystem,
                                String eventType,
                                Customer customer,
                                Order order) {

    public static final String SOURCE_JAVA_BACKEND = "JAVA_BACKEND";
    public static final String EVENT_ORDER_PLACED = "ORDER_PLACED";

    public record Customer(String name,
                           String email,
                           String membershipNo,
                           Boolean cardholder) {
    }

    public record Order(Integer orderId,
                        String orderNo,
                        LocalDateTime orderDate,
                        String orderStatus,
                        String paymentStatus,
                        BigDecimal subtotalMrp,
                        BigDecimal subtotalAmount,
                        BigDecimal totalSavings,
                        BigDecimal totalAmount,
                        Integer pointsRedeemed,
                        Integer pointsEarned,
                        List<Item> items,
                        Address shippingAddress,
                        Address billingAddress) {
    }

    public record Item(Integer prodId,
                       String prodName,
                       Integer quantity,
                       BigDecimal mrpPrice,
                       BigDecimal cardholderPrice,
                       String priceOption,
                       BigDecimal priceCharged,
                       BigDecimal lineTotal,
                       BigDecimal lineSavings,
                       Integer pointsRedeemed) {
    }

    public record Address(String addressLine1,
                          String addressLine2,
                          String city,
                          String state,
                          String zipCode,
                          String country) {
    }

    /**
     * Builds the payload from an already-mapped OrderResponse.
     *
     * MUST BE CALLED INSIDE THE TRANSACTION that placed the order. OrderResponse
     * is a flat DTO by then, but the email address comes off the User entity,
     * and with spring.jpa.open-in-view=false there is no session left to load
     * anything once placeOrder() returns.
     *
     * @param customerEmail from the User entity; OrderResponse deliberately
     *                      does not carry it, because the frontend has no use
     *                      for the address of the person already logged in.
     */
    public static OrderEmailPayload from(OrderResponse order, String customerEmail) {

        List<Item> items = order.getItems().stream()
                .map(OrderEmailPayload::toItem)
                .toList();

        Order orderPart = new Order(
                order.getOrderId(),
                order.getOrderNo(),
                order.getOrderDate(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getSubtotalMrp(),
                order.getSubtotalAmount(),
                order.getTotalSavings(),
                order.getTotalAmount(),
                order.getPointsRedeemed(),
                order.getPointsEarned(),
                items,
                toAddress(order.getShippingAddress()),
                toAddress(order.getBillingAddress()));

        Customer customer = new Customer(
                order.getCustomerName(),
                customerEmail,
                order.getMembershipNo(),
                order.getCardholder());

        return new OrderEmailPayload(SOURCE_JAVA_BACKEND, EVENT_ORDER_PLACED, customer, orderPart);
    }

    private static Item toItem(OrderDetailResponse item) {
        return new Item(
                item.getProdId(),
                item.getProdName(),
                item.getQuantity(),
                item.getMrpPrice(),
                item.getCardholderPrice(),
                // The enum travels as its NAME so the email service never has
                // to keep a PriceOption enum in step with two backends.
                item.getPriceOption() == null ? null : item.getPriceOption().name(),
                item.getPriceCharged(),
                item.getLineTotal(),
                item.getLineSavings(),
                item.getPointsRedeemed());
    }

    private static Address toAddress(AddressResponse address) {
        if (address == null) return null;
        return new Address(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry());
    }
}
