package com.joyopi.payment.temporal.activity;

import com.joyopi.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentActivityImpl implements PaymentActivity {

    private final PaymentService paymentService;

    @Override
    public void processPayment(Long orderId, Long amount) {
        log.info("PaymentActivity - 결제 처리 시작 (orderId: {}, amount: {})", orderId, amount);
        paymentService.pay(orderId, amount);
    }

    @Override
    public void cancelPayment(Long orderId, Long amount) {
        log.info("PaymentActivity - 결제 취소(보상) 시작 (orderId: {}, amount: {})", orderId, amount);
        paymentService.cancelPayment(orderId, amount);
    }
}
