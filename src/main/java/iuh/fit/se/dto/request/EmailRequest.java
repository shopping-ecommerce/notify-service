package iuh.fit.se.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailRequest {
    Sender sender;
    List<Recipient> to;
    String subject;
    String htmlContent;
}