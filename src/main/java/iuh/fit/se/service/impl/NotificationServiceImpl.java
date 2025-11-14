package iuh.fit.se.service.impl;

import iuh.fit.se.entity.Notification;
import iuh.fit.se.entity.enums.NotificationStatus;
import iuh.fit.se.entity.enums.NotificationType;
import iuh.fit.se.repository.NotificationRepository;
import iuh.fit.se.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NotificationServiceImpl implements NotificationService {
    NotificationRepository notificationRepository;

    /**
     * Tạo một thông báo mới
     */
    @Override
    public Notification createNotification(String userId, NotificationType type, Map<String, Object> content) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .content(content)
                .status(NotificationStatus.UNREAD) // Mặc định là chưa đọc
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Lấy danh sách thông báo của người dùng (có phân trang)
     */
    @Override
    public Page<Notification> getNotificationsByUserId(String userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    /**
     * Lấy số lượng thông báo chưa đọc của người dùng
     */
    @Override
    public long getUnreadNotificationCount(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    /**
     * Đánh dấu một thông báo là đã đọc
     * @param notificationId ID của thông báo cần đánh dấu
     * @return Optional chứa thông báo đã cập nhật nếu tìm thấy, ngược lại trả về Optional rỗng.
     */
    @Override
    public Optional<Notification> markAsRead(String notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setStatus(NotificationStatus.READ);
                    return notificationRepository.save(notification);
                });
    }

    /**
     * Đánh dấu tất cả thông báo của một người dùng là đã đọc.
     * Hiệu quả hơn việc lặp và lưu từng cái một.
     * @param userId ID của người dùng
     * @return Số lượng thông báo đã được cập nhật
     */
    @Override
    public long markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);

        if (unreadNotifications.isEmpty()) {
            return 0;
        }

        unreadNotifications.forEach(notification -> notification.setStatus(NotificationStatus.READ));

        notificationRepository.saveAll(unreadNotifications); // Lưu tất cả trong một lần gọi DB
        return unreadNotifications.size();
    }

    /**
     * Xóa một thông báo theo ID
     * @param notificationId ID của thông báo
     * @return true nếu xóa thành công, false nếu không tìm thấy thông báo
     */
    @Override
    public boolean deleteNotification(String notificationId) {
        if (notificationRepository.existsById(notificationId)) {
            notificationRepository.deleteById(notificationId);
            return true;
        }
        return false;
    }
}
