package com.joyopi.payment.repository;

import com.joyopi.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OutboxEvent 엔티티 처리를 위한 레포지토리 인터페이스
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
