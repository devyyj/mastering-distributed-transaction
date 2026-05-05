package com.joyopi.payment.service;

import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedPayment(Long orderId, Long amount) {
        log.info("결제 실패 상태 저장 시작 - orderId: {}", orderId);
        Payment payment = Payment.createFailed(orderId, amount);
        paymentRepository.save(payment);
        log.info("결제 실패 상태 저장 완료 - orderId: {}", orderId);
    }
}
