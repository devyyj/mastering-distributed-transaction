package com.joyopi.point.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import com.joyopi.point.domain.Point;
import com.joyopi.point.domain.OutboxEvent;
import com.joyopi.point.repository.PointRepository;
import com.joyopi.point.repository.OutboxRepository;
import com.joyopi.point.event.PointDeductedEvent;
import com.joyopi.point.event.PointRestoredEvent;
import com.joyopi.point.event.PointDeductionFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 포인트 사용 요청 처리
     */
    @Transactional
    public void usePoint(Long orderId, Long userId, Long amount, Long paymentAmount) {
        log.info("포인트 사용 요청 시작 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);
        try {
            Point point = pointRepository.findById(userId)
                    .orElseGet(() -> {
                        log.info("신규 사용자 포인트 생성 - userId: {}", userId);
                        return Point.create(userId);
                    });

            point.use(amount);
            pointRepository.save(point);
            log.info("포인트 사용 처리 완료 - userId: {}, balance: {}", userId, point.getBalance());

            // 2. 포인트 차감 성공 이벤트를 Outbox 테이블에 저장 (로컬 트랜잭션 통합)
            PointDeductedEvent deductedEvent = new PointDeductedEvent(orderId, userId, paymentAmount, amount);
            String payload = objectMapper.writeValueAsString(deductedEvent);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "point",
                    String.valueOf(userId),
                    "PointDeductedEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("포인트 차감 성공 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", orderId, outboxEvent.getId());

        } catch (BusinessException e) {
            log.error("포인트 사용 중 비즈니스 예외 발생 - userId: {}, errorCode: {}, message: {}", 
                    userId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("포인트 사용 중 예상치 못한 예외 발생 - userId: {}", userId, e);
            throw new RuntimeException("포인트 차감 실패", e);
        }
    }

    /**
     * 포인트 복구 요청 처리 (보상 트랜잭션)
     */
    @Transactional
    public void restorePoint(Long orderId, Long userId, Long amount) {
        log.info("보상 트랜잭션: 포인트 복구 요청 시작 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);
        try {
            Point point = pointRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("포인트 복구 실패: 사용자를 찾을 수 없음 - userId: {}", userId);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND);
                    });

            point.restore(amount);
            pointRepository.save(point);
            log.info("포인트 복구 처리 완료 - userId: {}, balance: {}", userId, point.getBalance());

            // 2. 포인트 복원 성공 이벤트를 Outbox 테이블에 저장 (로컬 트랜잭션 통합)
            PointRestoredEvent restoredEvent = new PointRestoredEvent(orderId, userId, amount);
            String payload = objectMapper.writeValueAsString(restoredEvent);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "point",
                    String.valueOf(userId),
                    "PointRestoredEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("포인트 복원 완료 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", orderId, outboxEvent.getId());

        } catch (BusinessException e) {
            log.error("포인트 복구 중 비즈니스 예외 발생 - userId: {}, errorCode: {}, message: {}", 
                    userId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("포인트 복구 중 예상치 못한 예외 발생 - userId: {}", userId, e);
            throw new RuntimeException("포인트 복원 실패", e);
        }
    }

    /**
     * 포인트 차감 실패 이벤트를 별도의 독립 트랜잭션으로 Outbox에 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePointDeductionFailedOutbox(Long orderId, String reason) {
        log.info("포인트 차감 실패 아웃박스 적재 시작 - orderId: {}, reason: {}", orderId, reason);
        try {
            PointDeductionFailedEvent failedEvent = new PointDeductionFailedEvent(orderId, reason);
            String payload = objectMapper.writeValueAsString(failedEvent);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "point",
                    String.valueOf(orderId),
                    "PointDeductionFailedEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("포인트 차감 실패 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", orderId, outboxEvent.getId());
        } catch (Exception e) {
            log.error("포인트 차감 실패 아웃박스 적재 중 예외 발생 - orderId: {}", orderId, e);
        }
    }
}

