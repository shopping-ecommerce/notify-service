package iuh.fit.se.controller;

import iuh.fit.se.dto.request.CreateNotificationRequest;
import iuh.fit.se.dto.response.ApiResponse;
import iuh.fit.se.entity.Notification;
import iuh.fit.se.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NotificationController {
    NotificationService notificationService;

    // Endpoint: POST /api/v1/notifications
    // Mục đích: Tạo một thông báo mới
    @PostMapping("/create")
    public ApiResponse<Notification> createNotification(@RequestBody CreateNotificationRequest request) {
        Notification notification = notificationService.createNotification(
                request.getUserId(),
                request.getType(),
                request.getContent()
        );
        return ApiResponse.<Notification>builder()
                .code(200)
                .message("Notification created successfully.")
                .result(notification)
                .build();
    }

    // Endpoint: GET /api/v1/notifications/user/{userId}
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<Notification>> getNotifications(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<Notification> notifications = notificationService.getNotificationsByUserId(userId, pageable);

        return ApiResponse.<Page<Notification>>builder()
                .message("Notifications retrieved successfully.")
                .result(notifications)
                .build();

    }

    // Endpoint: GET /api/v1/notifications/user/{userId}/unread-count
    @GetMapping("/user/{userId}/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadNotificationCount(userId);

        return ApiResponse.<Map<String, Long>>builder()
                .code(200)
                .message("Unread count retrieved successfully.")
                .result(Map.of("unreadCount", count))
                .build();

    }

    // Endpoint: PATCH /api/v1/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable String id) {
        return notificationService.markAsRead(id)
                .map(notification -> {
                    return ApiResponse.<Notification>builder()
                            .message("Notification marked as read successfully.")
                            .result(notification)
                            .build();
                })
                .orElseGet(() -> {
                    return ApiResponse.<Notification>builder()
                            .code(404) // Bạn có thể định nghĩa các mã lỗi riêng
                            .message("Notification with id " + id + " not found.")
                            .build();
                });
    }

    // Endpoint: POST /api/v1/notifications/user/{userId}/mark-all-as-read
    @PostMapping("/user/{userId}/mark-all-as-read")
    public ApiResponse<Map<String, Long>> markAllAsRead(@PathVariable String userId) {
        long updatedCount = notificationService.markAllAsRead(userId);

        return ApiResponse.<Map<String, Long>>builder()
                .code(200)
                .message("All unread notifications for user " + userId + " have been marked as read.")
                .result(Map.of("updatedCount", updatedCount))
                .build();
    }

    // Endpoint: DELETE /api/v1/notifications/{id}
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable String id) {
        boolean deleted = notificationService.deleteNotification(id);
        if (deleted) {
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("Notification with id " + id + " deleted successfully.")
                    .build();
        } else {
            return ApiResponse.<Void>builder()
                    .code(404)
                    .message("Notification with id " + id + " not found.")
                    .build();
        }
    }
}