package com.joyopi.point.service;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import com.joyopi.point.domain.Point;
import com.joyopi.point.repository.PointRepository;
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
    @DisplayName("TCC-Try: 신규 사용자가 포인트를 예약할 경우 10,000포인트에서 예약된다")
    void tryUsePoint_newUser() {
        // given
        Long userId = 1L;
        Long useAmount = 3000L;
        given(pointRepository.findById(userId)).willReturn(Optional.empty());
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.tryUsePoint(userId, useAmount);

        // then
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("TCC-Confirm: 포인트 예약을 확정한다")
    void confirmUsePoint() {
        // given
        Long userId = 1L;
        Long amount = 3000L;
        Point point = Point.create(userId);
        point.tryUse(amount);
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when
        pointService.confirmUsePoint(userId, amount);

        // then
        assertThat(point.getReservedPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("TCC-Cancel: 포인트 예약을 취소한다")
    void cancelUsePoint() {
        // given
        Long userId = 1L;
        Long amount = 3000L;
        Point point = Point.create(userId);
        point.tryUse(amount);
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when
        pointService.cancelUsePoint(userId, amount);

        // then
        assertThat(point.getBalance()).isEqualTo(10000L);
        assertThat(point.getReservedPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 잔액이 부족하면 예약이 실패한다")
    void tryUsePoint_insufficientBalance() {
        // given
        Long userId = 1L;
        Long useAmount = 15000L;
        Point point = Point.create(userId); // 10,000 balance
        given(pointRepository.findById(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.tryUsePoint(userId, useAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

}
