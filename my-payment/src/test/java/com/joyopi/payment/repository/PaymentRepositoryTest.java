package com.joyopi.payment.repository;

import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Payment 엔티티가 정상적으로 저장되고 조회된다")
    void saveAndFindPayment() {
        // given
        Payment payment = Payment.createReserved(1L, 10000L);

        // when
        Payment savedPayment = paymentRepository.save(payment);

        // then
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.RESERVED);
        
        Payment foundPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(foundPayment.getStatus()).isEqualTo(PaymentStatus.RESERVED);
    }
}
