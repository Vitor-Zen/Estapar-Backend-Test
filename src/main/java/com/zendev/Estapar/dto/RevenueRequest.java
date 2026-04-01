package com.zendev.Estapar.dto;

import java.time.LocalDate;

public record RevenueRequest(
        LocalDate date,
        String sector
) {
}
