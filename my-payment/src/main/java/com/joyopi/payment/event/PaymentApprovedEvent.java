package com.joyopi.payment.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovedEvent {
    private Long orderId;
    private Long paymentId;
    private Long userId;
    private Long usePoint;
    private String idempotencyKey;
}
