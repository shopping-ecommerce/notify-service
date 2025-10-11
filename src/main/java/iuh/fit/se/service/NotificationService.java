package iuh.fit.se.service;

import iuh.fit.se.entity.Notification;
import iuh.fit.se.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface NotificationService {
    Notification createNotification(String userId, NotificationType type, Map<String, Object> content);

    Page<Notification> getNotificationsByUserId(String userId, Pageable pageable);

    long getUnreadNotificationCount(String userId);

    Optional<Notification> markAsRead(String notificationId);

    long markAllAsRead(String userId);

    boolean deleteNotification(String notificationId);
}
