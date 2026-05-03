package com.joyopi.monolith.point.service;

import com.joyopi.monolith.point.domain.Point;
import com.joyopi.monolith.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 서비스
 */
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
        Point point = pointRepository.findById(userId)
                .orElseGet(() -> Point.create(userId));

        point.use(amount);
        pointRepository.save(point);
    }
}
