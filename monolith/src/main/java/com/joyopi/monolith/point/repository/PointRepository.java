package com.joyopi.monolith.point.repository;

import com.joyopi.monolith.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 포인트 레포지토리
 */
public interface PointRepository extends JpaRepository<Point, Long> {
}
