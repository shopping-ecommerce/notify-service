package iuh.fit.se.dto.response;

import iuh.fit.se.dto.request.Recipient;
import iuh.fit.se.dto.request.Sender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailReponse {
    Sender sender;
    List<Recipient> to;
    String subject;
    String htmlContent;
}
