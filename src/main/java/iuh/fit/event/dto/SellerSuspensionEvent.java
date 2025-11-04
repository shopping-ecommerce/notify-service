package iuh.fit.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerSuspensionEvent {
    private String sellerId;
    private String sellerEmail;
    private String violationType;
    private Integer violationCount;   // đổi int -> Integer
    private String reason;
    private LocalDateTime suspensionEndDate;
    private Integer suspensionDays;   // đổi int -> Integer
}