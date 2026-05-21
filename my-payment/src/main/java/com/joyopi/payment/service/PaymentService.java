package com.joyopi.payment.service;

import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryService paymentHistoryService;

    /**
     * 결제 요청 처리
     */
    @Transactional
    public void pay(Long orderId, Long amount) {
        log.info("결제 요청 시작 - orderId: {}, amount: {}", orderId, amount);

        try {
            validateAmount(amount);

            // 외부 결제 API 시뮬레이션 (딜레이)
            simulateExternalPayment();

            Payment payment = Payment.createSuccess(orderId, amount);
            paymentRepository.save(payment);
            log.info("결제 처리 성공 - orderId: {}", orderId);

        } catch (BusinessException e) {
            log.error("결제 처리 중 비즈니스 예외 발생 - orderId: {}, errorCode: {}, message: {}", 
                    orderId, e.getErrorCode().getCode(), e.getMessage());
            paymentHistoryService.saveFailedPayment(orderId, amount);
            throw e;
        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 예외 발생 - orderId: {}", orderId, e);
            paymentHistoryService.saveFailedPayment(orderId, amount);
            throw e;
        }
    }

    private void validateAmount(Long amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (amount > 1000000) {
            throw new BusinessException(ErrorCode.PAYMENT_LIMIT_EXCEEDED);
        }
    }

    private void simulateExternalPayment() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 결제 취소 처리 (보상 트랜잭션)
     */
    @Transactional
    public void cancelPayment(Long orderId, Long amount) {
        log.info("보상 트랜잭션: 결제 취소 요청 시작 - orderId: {}, amount: {}", orderId, amount);
        try {
            // 외부 결제 취소 API 연동 시뮬레이션
            simulateExternalPayment();
            
            Payment payment = Payment.createCanceled(orderId, amount);
            paymentRepository.save(payment);
            log.info("결제 취소 처리 성공 - orderId: {}", orderId);
        } catch (Exception e) {
            log.error("결제 취소 중 예외 발생 - orderId: {}", orderId, e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
