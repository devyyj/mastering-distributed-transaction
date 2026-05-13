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
     * TCC - Try: 포인트 사용 예약
     */
    @Transactional
    public void tryUsePoint(Long userId, Long amount) {
        log.info("TCC-Try: 포인트 사용 예약 시작 - userId: {}, amount: {}", userId, amount);
        Point point = pointRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("신규 사용자 포인트 생성 - userId: {}", userId);
                    return pointRepository.save(Point.create(userId));
                });

        point.tryUse(amount);
        log.info("TCC-Try: 포인트 사용 예약 완료 - userId: {}, balance: {}, reserved: {}", 
                userId, point.getBalance(), point.getReservedPoint());
    }

    /**
     * TCC - Confirm: 포인트 사용 확정
     */
    @Transactional
    public void confirmUsePoint(Long userId, Long amount) {
        log.info("TCC-Confirm: 포인트 사용 확정 시작 - userId: {}, amount: {}", userId, amount);
        Point point = pointRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        point.confirmUse(amount);
        log.info("TCC-Confirm: 포인트 사용 확정 완료 - userId: {}, balance: {}, reserved: {}", 
                userId, point.getBalance(), point.getReservedPoint());
    }

    /**
     * TCC - Cancel: 포인트 사용 예약 취소
     */
    @Transactional
    public void cancelUsePoint(Long userId, Long amount) {
        log.info("TCC-Cancel: 포인트 사용 예약 취소 시작 - userId: {}, amount: {}", userId, amount);
        Point point = pointRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        point.cancelUse(amount);
        log.info("TCC-Cancel: 포인트 사용 예약 취소 완료 - userId: {}, balance: {}, reserved: {}", 
                userId, point.getBalance(), point.getReservedPoint());
    }

}
