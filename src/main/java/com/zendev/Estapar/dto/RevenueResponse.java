package com.zendev.Estapar.dto;

import java.math.BigDecimal;

public record RevenueResponse(
        BigDecimal amount,
        String currency,
        String timestamp
) {
}
