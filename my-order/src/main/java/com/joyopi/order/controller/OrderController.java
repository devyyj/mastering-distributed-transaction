package com.joyopi.order.controller;

import com.joyopi.order.common.response.ApiResponse;
import com.joyopi.order.controller.dto.OrderRequest;
import com.joyopi.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * 상품 주문 요청
     */
    @PostMapping
    public ApiResponse<Long> order(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderRequest request) {
        Long orderId = orderService.order(request.toCommand(idempotencyKey));
        return ApiResponse.success("주문이 완료되었습니다.", orderId);
    }
}
