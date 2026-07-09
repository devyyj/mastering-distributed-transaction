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
    public void saveFailedPayment(Long orderId, Long amount, String idempotencyKey) {
        log.info("결제 실패 상태 저장 시작 - orderId: {}", orderId);
        String failedIdempotencyKey = idempotencyKey + "-failed";

        // 멱등성 검증
        if (paymentRepository.findByIdempotencyKey(failedIdempotencyKey).isPresent()) {
            log.info("결제 실패 상태 저장 중복 요청 무시 - orderId: {}", orderId);
            return;
        }

        try {
            Payment payment = Payment.createFailed(orderId, amount, failedIdempotencyKey);
            paymentRepository.save(payment);
            paymentRepository.flush();
            log.info("결제 실패 상태 저장 완료 - orderId: {}", orderId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.info("동시 요청으로 인한 결제 실패 상태 저장 중복 처리 방지 - orderId: {}", orderId);
        }
    }
}
