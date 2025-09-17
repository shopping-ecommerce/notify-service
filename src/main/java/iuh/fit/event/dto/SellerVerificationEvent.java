package iuh.fit.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerVerificationEvent {
    String sellerId;
    String sellerEmail;
    String status; // APPROVED or REJECTED
    String reason; // Optional, for rejection reason
}