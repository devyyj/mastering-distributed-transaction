package com.joyopi.point.repository;

import com.joyopi.point.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 포인트 이력 레포지토리
 */
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Optional<PointHistory> findByIdempotencyKey(String idempotencyKey);
}
