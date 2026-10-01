package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pulse.pass.domain.TicketType;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.service.impl.TicketPriceCalculator;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    @Test
    void shouldReturnPriceForEachTicketType() {
        assertThat(calculator.calculatePrice(TicketType.GENERAL)).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(calculator.calculatePrice(TicketType.STUDENT)).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(calculator.calculatePrice(TicketType.VIP)).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(calculator.calculatePrice(TicketType.BACKSTAGE)).isEqualByComparingTo(new BigDecimal("200000.00"));
    }

    @Test
    void shouldRejectMissingTicketType() {
        assertThatThrownBy(() -> calculator.calculatePrice(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Unsupported ticket type: null");
    }
}