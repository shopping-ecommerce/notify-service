package iuh.fit.se.entity;

import iuh.fit.se.entity.enums.NotificationStatus;
import iuh.fit.se.entity.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "notifications")
public class Notification {

    @Id
     String id; // Spring Data sẽ tự động map trường này với _id trong MongoDB

    @Field("user_id")
    @Indexed // Đánh index cho trường này để tăng tốc độ truy vấn theo userId
    String userId;

    NotificationType type;

    // Sử dụng Map để có nội dung linh hoạt, giống như JSON object
    Map<String, Object> content;

    @Field("is_read")
    @Indexed // Đánh index để lọc nhanh các thông báo chưa đọc
    NotificationStatus status;

    // Annotation này sẽ tự động tạo TTL Index trong MongoDB.
    // Document sẽ bị xóa sau 30 ngày (2592000 giây) kể từ thời điểm tạo.
    @Indexed(name = "created_at_ttl_index", expireAfterSeconds = 2592000)
    @Field("created_at")
            @CreatedDate
    Instant createdAt;
}