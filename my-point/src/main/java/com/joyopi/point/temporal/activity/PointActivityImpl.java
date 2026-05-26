package com.joyopi.point.temporal.activity;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.service.PointService;
import io.temporal.failure.ApplicationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointActivityImpl implements PointActivity {

    private final PointService pointService;

    @Override
    public void usePoint(Long userId, Long amount) {
        log.info("PointActivity - 포인트 차감 시작 (userId: {}, amount: {})", userId, amount);
        try {
            pointService.usePoint(userId, amount);
        } catch (BusinessException e) {
            log.error("PointActivity - 포인트 사용 중 비즈니스 예외 발생 (재시도 중단): {}", e.getMessage());
            // 비즈니스 예외(잔액 부족 등)는 아무리 재시도해도 다시 실패하므로 Non-Retryable ApplicationFailure로 감싸서 즉시 종료시킵니다.
            // Java 17+ 환경에서 Throwable(cause) 직렬화 시 모듈 시스템(Reflection) 제약으로 인한 직렬화 예외 방지를 위해 cause를 제외합니다.
            throw ApplicationFailure.newNonRetryableFailure(
                    e.getMessage(),
                    e.getClass().getName()
            );
        }
    }

    @Override
    public void restorePoint(Long userId, Long amount) {
        log.info("PointActivity - 포인트 복구(보상) 시작 (userId: {}, amount: {})", userId, amount);
        pointService.restorePoint(userId, amount);
    }
}
