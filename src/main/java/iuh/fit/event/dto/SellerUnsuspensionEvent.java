// module: event-dto
package iuh.fit.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerUnsuspensionEvent {
    String sellerId;
    String sellerEmail;
    LocalDateTime unsuspendedAt;
}
