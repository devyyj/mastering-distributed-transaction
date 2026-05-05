package com.joyopi.point.domain;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {

    @Test
    @DisplayName("포인트를 복구하면 잔액이 증가한다")
    void restore() {
        // given
        Point point = Point.create(1L); // initial 10,000
        point.use(3000L); // balance 7,000

        // when
        point.restore(3000L);

        // then
        assertThat(point.getBalance()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("음수 금액을 복구하려 하면 예외가 발생한다")
    void restore_negativeAmount() {
        // given
        Point point = Point.create(1L);

        // when & then
        assertThatThrownBy(() -> point.restore(-1000L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
