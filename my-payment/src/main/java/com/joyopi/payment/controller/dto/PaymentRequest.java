package com.joyopi.payment.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private Long amount;
}
