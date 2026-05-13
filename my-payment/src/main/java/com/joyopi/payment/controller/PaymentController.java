package com.joyopi.payment.controller;

import com.joyopi.payment.common.response.ApiResponse;
import com.joyopi.payment.controller.dto.PaymentRequest;
import com.joyopi.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/try")
    public ApiResponse<Void> tryPay(@RequestBody PaymentRequest request) {
        paymentService.tryPay(request.getOrderId(), request.getAmount());
        return ApiResponse.success(null);
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirmPay(@RequestBody PaymentRequest request) {
        paymentService.confirmPay(request.getOrderId());
        return ApiResponse.success(null);
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancelPay(@RequestBody PaymentRequest request) {
        paymentService.cancelPay(request.getOrderId());
        return ApiResponse.success(null);
    }

}
