package com.pulse.pass.service.impl;

import com.pulse.pass.domain.TicketType;
import com.pulse.pass.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TicketPriceCalculator {

    private static final Map<TicketType, BigDecimal> PRICES = Map.of(
            TicketType.GENERAL, new BigDecimal("100000.00"),
            TicketType.STUDENT, new BigDecimal("80000.00"),
            TicketType.VIP, new BigDecimal("150000.00"),
            TicketType.BACKSTAGE, new BigDecimal("200000.00"));

    public BigDecimal calculatePrice(TicketType type) {
        if (type == null) {
            throw new BusinessRuleException("Unsupported ticket type: null");
        }
        BigDecimal price = PRICES.get(type);
        if (price == null) {
            throw new BusinessRuleException("Unsupported ticket type: " + type);
        }
        return price;
    }
}