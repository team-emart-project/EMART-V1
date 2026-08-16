package com.example.demo.controller;

import com.example.demo.dto.request.AddressDto;
import com.example.demo.dto.request.CustomerDto;
import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.dto.request.OrderInvoiceDto;
import com.example.demo.dto.request.OrderItemDto;
import com.example.demo.dto.response.EmailSendResponse;
import com.example.demo.service.interfaces.OrderEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
// Jackson 3 (tools.jackson), not com.fasterxml.jackson.databind - Spring Boot 4
// moved to the new coordinates. Only the ANNOTATIONS still live under
// com.fasterxml, which is why @JsonInclude in the DTOs keeps its old import.
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The endpoint's contract, from the caller's side.
 *
 * The service is mocked: what matters here is that a good payload is accepted
 * with 202 and a bad one is rejected with a field-level 400 — the two things
 * the Java and .NET clients actually depend on.
 */
@WebMvcTest(OrderEmailController.class)
class OrderEmailControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderEmailService orderEmailService;

    @Test
    void acceptsAValidOrderPayload() throws Exception {
        given(orderEmailService.acceptOrderPlaced(any()))
                .willReturn(EmailSendResponse.accepted("abc12345", "ORD-2026-000001", "r***j@gmail.com"));

        mockMvc.perform(post("/api/send-order-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.orderNo").value("ORD-2026-000001"));

        verify(orderEmailService).acceptOrderPlaced(any());
    }

    @Test
    void rejectsAnOrderWithNoItems() throws Exception {
        OrderEmailRequest broken = new OrderEmailRequest(
                "JAVA_BACKEND", "ORDER_PLACED",
                new CustomerDto("Test User", "test@example.com", "EM-1", false),
                new OrderInvoiceDto(1, "ORD-2026-000002", LocalDateTime.now(), "PLACED", "PENDING",
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, 0, 0,
                        List.of(), null, null));

        mockMvc.perform(post("/api/send-order-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors['order.items']").exists());

        // Nothing was queued: a payload this service cannot render must never
        // reach the mail thread.
        verify(orderEmailService, never()).acceptOrderPlaced(any());
    }

    @Test
    void rejectsACustomerWithAMalformedEmail() throws Exception {
        OrderEmailRequest broken = new OrderEmailRequest(
                "DOTNET_BACKEND", "ORDER_PLACED",
                new CustomerDto("Test User", "not-an-email", null, false),
                validRequest().order());

        mockMvc.perform(post("/api/send-order-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['customer.email']").exists());

        verify(orderEmailService, never()).acceptOrderPlaced(any());
    }

    private OrderEmailRequest validRequest() {
        OrderItemDto item = new OrderItemDto(
                7, "Wireless Headphones", 2,
                new BigDecimal("2499.00"), new BigDecimal("1999.00"), "CARDHOLDER",
                new BigDecimal("1999.00"), new BigDecimal("3998.00"), new BigDecimal("1000.00"), 0);

        OrderInvoiceDto order = new OrderInvoiceDto(
                41, "ORD-2026-000001", LocalDateTime.of(2026, 8, 10, 14, 22, 31),
                "PLACED", "PENDING",
                new BigDecimal("4998.00"), new BigDecimal("3998.00"),
                new BigDecimal("1000.00"), new BigDecimal("3998.00"),
                0, 119,
                List.of(item),
                new AddressDto("12 MG Road", null, "Indore", "Madhya Pradesh", "452001", "India"),
                null);

        return new OrderEmailRequest("JAVA_BACKEND", "ORDER_PLACED",
                new CustomerDto("Rishiraj Chhalotre", "rishiraj@example.com", "EM-000123", true),
                order);
    }
}
