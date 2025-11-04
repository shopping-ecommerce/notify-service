package iuh.fit.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerWarningEvent {
    private String sellerId;
    private String sellerEmail;
    private String violationType;
    private Integer violationCount;   // đổi int -> Integer
    private String warningMessage;
}