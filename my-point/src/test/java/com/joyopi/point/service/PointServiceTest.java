package com.joyopi.point.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import com.joyopi.point.domain.Point;
import com.joyopi.point.domain.OutboxEvent;
import com.joyopi.point.repository.PointRepository;
import com.joyopi.point.repository.PointHistoryRepository;
import com.joyopi.point.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("신규 사용자가 포인트를 사용할 경우 10,000포인트에서 차감되고 아웃박스 이벤트가 저장된다")
    void usePoint_newUser() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long useAmount = 3000L;
        Long paymentAmount = 7000L;
        String idempotencyKey = "idemp-key-use-1";

        given(pointHistoryRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());
        given(pointRepository.findById(userId)).willReturn(java.util.Optional.empty());
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        pointService.usePoint(orderId, userId, useAmount, paymentAmount, idempotencyKey);

        // then
        verify(pointHistoryRepository).save(any(com.joyopi.point.domain.PointHistory.class));
        verify(pointRepository).save(any(Point.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("포인트 잔액이 부족하면 예외가 발생한다")
    void usePoint_insufficientBalance() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long useAmount = 15000L;
        Long paymentAmount = 0L;
        String idempotencyKey = "idemp-key-use-2";
        Point point = Point.create(userId); // 10,000 balance

        given(pointHistoryRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());
        given(pointRepository.findById(userId)).willReturn(java.util.Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(orderId, userId, useAmount, paymentAmount, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

    @Test
    @DisplayName("음수 금액을 사용하려 하면 예외가 발생한다")
    void usePoint_negativeAmount() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long useAmount = -1000L;
        Long paymentAmount = 11000L;
        String idempotencyKey = "idemp-key-use-3";

        given(pointHistoryRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(orderId, userId, useAmount, paymentAmount, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("동일한 멱등키로 포인트 사용이 중복 요청되면 비즈니스 로직을 실행하지 않고 리턴한다 (Idempotent Skip)")
    void usePoint_duplicate_shouldSkip() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long useAmount = 3000L;
        Long paymentAmount = 7000L;
        String idempotencyKey = "idemp-key-use-dup";
        com.joyopi.point.domain.PointHistory existingHistory = com.joyopi.point.domain.PointHistory.create(
                userId, orderId, useAmount, com.joyopi.point.domain.PointHistory.PointHistoryType.DEDUCT, idempotencyKey
        );

        given(pointHistoryRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.of(existingHistory));

        // when
        pointService.usePoint(orderId, userId, useAmount, paymentAmount, idempotencyKey);

        // then
        // 비즈니스 로직(PointRepository, OutboxRepository)이 전혀 호출되지 않아야 함
        org.mockito.Mockito.verifyNoInteractions(pointRepository);
        org.mockito.Mockito.verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("포인트를 복구하면 잔액이 증가하고 저장되며 아웃박스 이벤트가 생성된다")
    void restorePoint() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long restoreAmount = 3000L;
        String idempotencyKey = "idemp-key-restore-1";
        String restoreIdempotencyKey = idempotencyKey + "-restore";
        Point point = Point.create(userId); // 10,000 balance

        given(pointHistoryRepository.findByIdempotencyKey(restoreIdempotencyKey)).willReturn(java.util.Optional.empty());
        given(pointRepository.findById(userId)).willReturn(java.util.Optional.of(point));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        pointService.restorePoint(orderId, userId, restoreAmount, idempotencyKey);

        // then
        assertThat(point.getBalance()).isEqualTo(13000L);
        verify(pointHistoryRepository).save(any(com.joyopi.point.domain.PointHistory.class));
        verify(pointRepository).save(point);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("동일한 멱등키로 포인트 복구가 중복 요청되면 복구를 진행하지 않고 리턴한다 (Idempotent Skip)")
    void restorePoint_duplicate_shouldSkip() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long restoreAmount = 3000L;
        String idempotencyKey = "idemp-key-restore-dup";
        String restoreIdempotencyKey = idempotencyKey + "-restore";
        com.joyopi.point.domain.PointHistory existingHistory = com.joyopi.point.domain.PointHistory.create(
                userId, orderId, restoreAmount, com.joyopi.point.domain.PointHistory.PointHistoryType.RESTORE, restoreIdempotencyKey
        );

        given(pointHistoryRepository.findByIdempotencyKey(restoreIdempotencyKey)).willReturn(java.util.Optional.of(existingHistory));

        // when
        pointService.restorePoint(orderId, userId, restoreAmount, idempotencyKey);

        // then
        // 비즈니스 로직이 호출되지 않아야 함
        org.mockito.Mockito.verifyNoInteractions(pointRepository);
        org.mockito.Mockito.verifyNoInteractions(outboxRepository);
    }
}
