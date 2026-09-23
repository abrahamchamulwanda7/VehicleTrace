package com.vehicletrace;

import java.math.BigDecimal;
import java.util.List;

public record RepairRequest(
        String numberPlate,
        String problemDescription,
        String workDone,
        BigDecimal cost,
        String repairDate,
        List<PartRequest> parts
) {
    // One part used in the repair (FR7)
    public record PartRequest(
            String partName,
            Integer quantity,
            BigDecimal cost
    ) {
    }
}