package com.joyopi.point.temporal.activity;

import com.joyopi.point.service.PointService;
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
        pointService.usePoint(userId, amount);
    }

    @Override
    public void restorePoint(Long userId, Long amount) {
        log.info("PointActivity - 포인트 복구(보상) 시작 (userId: {}, amount: {})", userId, amount);
        pointService.restorePoint(userId, amount);
    }
}
