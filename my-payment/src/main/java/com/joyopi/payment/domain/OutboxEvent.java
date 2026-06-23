package com.joyopi.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 트랜잭셔널 아웃박스(Transactional Outbox) 패턴을 위한 엔티티
 * Debezium Outbox Event Router SMT의 기본 컬럼 명세를 따릅니다.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이벤트가 속한 애그리거트의 타입 (예: "payment")
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    // 애그리거트의 식별값 (예: orderId)
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    // 이벤트의 종류 (예: "PaymentApprovedEvent")
    @Column(name = "type", nullable = false)
    private String type;

    // JSON 형태의 이벤트 데이터 페이로드
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static OutboxEvent create(String aggregateType, String aggregateId, String type, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.type = type;
        event.payload = payload;
        event.createdAt = LocalDateTime.now();
        return event;
    }
}
