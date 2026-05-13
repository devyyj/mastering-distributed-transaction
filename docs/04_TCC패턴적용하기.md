# TCC(Try-Confirm-Cancel) 패턴 적용 계획

본 문서는 `my-*` 마이크로서비스에 분산 트랜잭션 관리를 위한 TCC 패턴을 적용하기 위한 계획을 정의합니다.

## 1. 개요

TCC(Try-Confirm-Cancel) 패턴은 2단계 커밋(2PC)의 대안으로, 비즈니스 로직 레벨에서 리소스를 예약하고 최종적으로 확정하거나 취소하는 방식입니다.

- **Try**: 가용 리소스를 확인하고 예약합니다. (예: 포인트 예약, 결제 대기 생성)
- **Confirm**: 예약된 리소스를 최종 사용 확정합니다. 모든 Try가 성공했을 때 실행합니다.
- **Cancel**: 예약된 리소스를 해제합니다. Try 중 하나라도 실패했을 때 실행합니다.

## 2. 서비스별 역할 및 단계별 정의

### 2.1 my-order (Coordinator & Participant)
주문 서비스는 TCC 전체 흐름을 제어하는 조정자 역할을 수행함과 동시에 주문 자체를 관리하는 참여자 역할을 합니다.

- **Try**: 주문 상태를 `RESERVED`로 생성합니다.
- **Confirm**: 주문 상태를 `COMPLETED`로 변경합니다.
- **Cancel**: 주문 상태를 `CANCELLED`로 변경합니다.

### 2.2 my-point (Participant)
- **Try**: 사용자의 포인트 잔액을 확인하고 사용 요청 금액만큼 `reserved_point`로 이동시킵니다. (가용 잔액은 줄어듦)
- **Confirm**: `reserved_point`를 차감하여 최종 사용을 확정합니다.
- **Cancel**: `reserved_point`를 다시 일반 포인트 잔액으로 복구합니다.

### 2.3 my-payment (Participant)
- **Try**: 결제 가능 여부를 확인하고 `RESERVED` 상태의 결제 기록을 생성합니다.
- **Confirm**: 결제 상태를 `COMPLETED`로 변경합니다. (실제 외부 결제 승인 완료)
- **Cancel**: 결제 상태를 `CANCELLED`로 변경합니다.

## 3. 상세 설계

### 3.1 API 명세 (신규 추가/수정)

#### my-point (포인트 서비스)
- `POST /api/points/try`: 포인트 예약
- `POST /api/points/confirm`: 포인트 사용 확정
- `POST /api/points/cancel`: 포인트 예약 취소

#### my-payment (결제 서비스)
- `POST /api/payments/try`: 결제 예약
- `POST /api/payments/confirm`: 결제 확정
- `POST /api/payments/cancel`: 결제 취소

### 3.2 데이터 모델 변경 사항
- **Point**: `reservedPoint` 필드 추가 필요.
- **Order / Payment**: `RESERVED` 상태 추가.

## 4. 실행 흐름 (Sequence)

1. **[Order]** `Try`: 주문 데이터 생성 (`RESERVED`)
2. **[Order -> Point]** `Try`: 포인트 예약 요청
3. **[Order -> Payment]** `Try`: 결제 예약 요청
4. **결정 단계**:
   - **모든 Try 성공 시**:
     1. **[Order -> Point]** `Confirm`: 포인트 확정
     2. **[Order -> Payment]** `Confirm`: 결제 확정
     3. **[Order]** `Confirm`: 주문 확정 (`COMPLETED`)
   - **하나라도 Try 실패 시**:
     1. **[Order -> Point]** `Cancel`: 포인트 취소 (성공했던 경우만)
     2. **[Order -> Payment]** `Cancel`: 결제 취소 (성공했던 경우만)
     3. **[Order]** `Cancel`: 주문 취소 (`CANCELLED`)

## 5. 단계별 실행 체크리스트

- [x] **도메인 모델 수정**: `Point` 엔티티에 `reservedPoint` 필드 추가
- [x] **상태값 추가**: `OrderStatus`, `PaymentStatus`에 `RESERVED` 추가
- [x] **my-point TCC 구현**: Try, Confirm, Cancel 로직 및 API 개발
- [x] **my-payment TCC 구현**: Try, Confirm, Cancel 로직 및 API 개발
- [x] **my-order Coordinator 구현**: TCC 흐름 제어 로직 개발
- [x] **테스트 및 검증**: 
  - [x] 정상 흐름 테스트
  - [x] 포인트 예약 실패 시 취소 흐름 테스트
  - [x] 결제 예약 실패 시 포인트 취소 흐름 테스트
  - [x] `msa-test.http` 업데이트 및 검증
