package com.joyopi.point.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import com.joyopi.point.domain.Point;
import com.joyopi.point.domain.OutboxEvent;
import com.joyopi.point.repository.PointRepository;
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
        given(pointRepository.findById(userId)).willReturn(Optional.empty());
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        pointService.usePoint(orderId, userId, useAmount, paymentAmount);

        // then
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
        Point point = Point.create(userId); // 10,000 balance
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(orderId, userId, useAmount, paymentAmount))
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
        Point point = Point.create(userId);
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(orderId, userId, useAmount, paymentAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("포인트를 복구하면 잔액이 증가하고 저장되며 아웃박스 이벤트가 생성된다")
    void restorePoint() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long restoreAmount = 3000L;
        Point point = Point.create(userId); // 10,000 balance
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        pointService.restorePoint(orderId, userId, restoreAmount);

        // then
        assertThat(point.getBalance()).isEqualTo(13000L);
        verify(pointRepository).save(point);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }
}
