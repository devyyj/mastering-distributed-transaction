package com.joyopi.payment.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    // Point
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "사용자를 찾을 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "P002", "포인트가 부족합니다."),

    // Payment
    PAYMENT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "M001", "결제 한도를 초과했습니다."),
    PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M002", "결제 처리에 실패했습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
