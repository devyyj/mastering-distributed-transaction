package com.joyopi.monolith.point.service;

import com.joyopi.monolith.common.exception.BusinessException;
import com.joyopi.monolith.common.exception.ErrorCode;
import com.joyopi.monolith.point.domain.Point;
import com.joyopi.monolith.point.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Test
    @DisplayName("신규 사용자가 포인트를 사용할 경우 10,000포인트에서 차감된다")
    void usePoint_newUser() {
        // given
        Long userId = 1L;
        Long useAmount = 3000L;
        given(pointRepository.findById(userId)).willReturn(Optional.empty());
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.usePoint(userId, useAmount);

        // then
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 잔액이 부족하면 예외가 발생한다")
    void usePoint_insufficientBalance() {
        // given
        Long userId = 1L;
        Long useAmount = 15000L;
        Point point = Point.create(userId); // 10,000 balance
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

    @Test
    @DisplayName("음수 금액을 사용하려 하면 예외가 발생한다")
    void usePoint_negativeAmount() {
        // given
        Long userId = 1L;
        Long useAmount = -1000L;
        Point point = Point.create(userId);
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
