package com.joyopi.point.domain;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {

    @Test
    @DisplayName("TCC-Try: 포인트를 예약하면 잔액은 줄어들고 예약금액은 늘어난다")
    void tryUse() {
        // given
        Point point = Point.create(1L); // initial 10,000

        // when
        point.tryUse(3000L);

        // then
        assertThat(point.getBalance()).isEqualTo(7000L);
        assertThat(point.getReservedPoint()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("TCC-Confirm: 예약을 확정하면 예약금액이 줄어든다")
    void confirmUse() {
        // given
        Point point = Point.create(1L);
        point.tryUse(3000L);

        // when
        point.confirmUse(3000L);

        // then
        assertThat(point.getBalance()).isEqualTo(7000L);
        assertThat(point.getReservedPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("TCC-Cancel: 예약을 취소하면 잔액이 복구되고 예약금액이 줄어든다")
    void cancelUse() {
        // given
        Point point = Point.create(1L);
        point.tryUse(3000L);

        // when
        point.cancelUse(3000L);

        // then
        assertThat(point.getBalance()).isEqualTo(10000L);
        assertThat(point.getReservedPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 예약하려 하면 예외가 발생한다")
    void tryUse_insufficientBalance() {
        // given
        Point point = Point.create(1L);

        // when & then
        assertThatThrownBy(() -> point.tryUse(15000L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

}
