package com.joyopi.point.service;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import com.joyopi.point.domain.Point;
import com.joyopi.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    /**
     * 포인트 사용 요청 처리
     */
    @Transactional
    public void usePoint(Long userId, Long amount) {
        log.info("포인트 사용 요청 시작 - userId: {}, amount: {}", userId, amount);
        try {
            Point point = pointRepository.findById(userId)
                    .orElseGet(() -> {
                        log.info("신규 사용자 포인트 생성 - userId: {}", userId);
                        return Point.create(userId);
                    });

            point.use(amount);
            pointRepository.save(point);
            log.info("포인트 사용 처리 완료 - userId: {}, balance: {}", userId, point.getBalance());
        } catch (BusinessException e) {
            log.error("포인트 사용 중 비즈니스 예외 발생 - userId: {}, errorCode: {}, message: {}", 
                    userId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("포인트 사용 중 예상치 못한 예외 발생 - userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 포인트 복구 요청 처리 (보상 트랜잭션)
     */
    @Transactional
    public void restorePoint(Long userId, Long amount) {
        log.info("보상 트랜잭션: 포인트 복구 요청 시작 - userId: {}, amount: {}", userId, amount);
        try {
            Point point = pointRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("포인트 복구 실패: 사용자를 찾을 수 없음 - userId: {}", userId);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND);
                    });

            point.restore(amount);
            pointRepository.save(point);
            log.info("포인트 복구 처리 완료 - userId: {}, balance: {}", userId, point.getBalance());
        } catch (BusinessException e) {
            log.error("포인트 복구 중 비즈니스 예외 발생 - userId: {}, errorCode: {}, message: {}", 
                    userId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("포인트 복구 중 예상치 못한 예외 발생 - userId: {}", userId, e);
            throw e;
        }
    }
}
