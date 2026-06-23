package com.joyopi.payment.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.domain.OutboxEvent;
import com.joyopi.payment.repository.PaymentRepository;
import com.joyopi.payment.repository.OutboxRepository;
import com.joyopi.payment.event.PaymentApprovedEvent;
import com.joyopi.payment.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 결제 요청 처리
     */
    @Transactional
    public void pay(Long orderId, Long userId, Long amount, Long usePoint) {
        log.info("결제 요청 시작 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);

        try {
            validateAmount(amount);

            // 외부 결제 API 시뮬레이션 (딜레이)
            simulateExternalPayment();

            Payment payment = Payment.createSuccess(orderId, amount);
            paymentRepository.save(payment);
            log.info("결제 처리 성공 - orderId: {}", orderId);

            // 결제 성공 이벤트를 Outbox 테이블에 저장 (로컬 트랜잭션 통합)
            PaymentApprovedEvent approvedEvent = new PaymentApprovedEvent(
                    orderId,
                    1L, // paymentId 임시값
                    userId,
                    usePoint
            );
            String payload = objectMapper.writeValueAsString(approvedEvent);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "payment",
                    String.valueOf(orderId),
                    "PaymentApprovedEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("결제 성공 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", orderId, outboxEvent.getId());

        } catch (BusinessException e) {
            log.error("결제 처리 중 비즈니스 예외 발생 - orderId: {}, errorCode: {}, message: {}", 
                    orderId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 예외 발생 - orderId: {}", orderId, e);
            throw new RuntimeException("결제 처리 실패", e);
        }
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount < 0) {
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

    /**
     * 결제 실패 이벤트를 별도의 독립 트랜잭션으로 Outbox에 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePaymentFailedOutbox(Long orderId, Long userId, Long usePoint, String reason) {
        log.info("결제 실패 아웃박스 적재 시작 - orderId: {}, reason: {}", orderId, reason);
        try {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    orderId,
                    userId,
                    usePoint,
                    reason
            );
            String payload = objectMapper.writeValueAsString(failedEvent);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "payment",
                    String.valueOf(orderId),
                    "PaymentFailedEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("결제 실패 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", orderId, outboxEvent.getId());
        } catch (Exception e) {
            log.error("결제 실패 아웃박스 적재 중 예외 발생 - orderId: {}", orderId, e);
        }
    }
}

