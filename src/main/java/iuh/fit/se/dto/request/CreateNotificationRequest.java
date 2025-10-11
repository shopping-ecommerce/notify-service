package iuh.fit.se.dto.request;

import iuh.fit.se.entity.enums.NotificationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateNotificationRequest {
    String userId;
    NotificationType type;
    Map<String, Object> content;
}
