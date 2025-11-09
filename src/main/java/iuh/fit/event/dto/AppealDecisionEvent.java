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
public class AppealDecisionEvent {
    String appealId;
    String sellerId;
    String sellerEmail;
    String appealType;
    String status;
    String adminResponse;
    LocalDateTime reviewedAt;
}