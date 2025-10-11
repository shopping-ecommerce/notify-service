package iuh.fit.se.controller;

import iuh.fit.event.dto.NotificationEvent;
import iuh.fit.event.dto.OrderCreatedEvent;
import iuh.fit.event.dto.OrderStatusChangedEvent;
import iuh.fit.event.dto.SellerVerificationEvent;
import iuh.fit.se.dto.request.Recipient;
import iuh.fit.se.dto.request.SendEmailRequest;
import iuh.fit.se.entity.enums.NotificationType;
import iuh.fit.se.service.NotificationService;
import iuh.fit.se.service.impl.EmailServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NotificationConsumer {
    EmailServiceImpl emailService;
    NotificationService notificationService;
    @KafkaListener(topics = "notification-delivery")
    public void listen(NotificationEvent notificationEvent) {
        log.info("Received notification event: {}", notificationEvent);
        emailService.sendEmail(SendEmailRequest.builder()
                        .subject(notificationEvent.getSubject())
                        .to(Recipient.builder()
                                .email(notificationEvent.getRecipient())
                                .build())
                        .htmlContent(notificationEvent.getBody())
                .build());
    }

    @KafkaListener(topics = "notification-delivery-success")
    public void success(NotificationEvent notificationEvent) {
        log.info("Received notification event: {}", notificationEvent);
        emailService.sendEmailSuccess(SendEmailRequest.builder()
                .subject(notificationEvent.getSubject())
                .to(Recipient.builder()
                        .email(notificationEvent.getRecipient())
                        .build())
                .htmlContent(notificationEvent.getBody())
                .build());
    }

    // Thêm vào NotificationController
    @KafkaListener(topics = "create-order",properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.OrderCreatedEvent"
    })
    public void handleOrderCreated(OrderCreatedEvent orderEvent) {
        String contentText = String.format("Đơn hàng #%s của bạn đã được tạo thành công.", orderEvent.getOrderId());
        Map<String, Object> content = Map.of(
                "text", contentText,
                "orderId", orderEvent.getOrderId(),
                "link", "/user/orders/" + orderEvent.getOrderId()
        );
        notificationService.createNotification(orderEvent.getUserId(), NotificationType.NOTIFY, content);

        log.info("Received OrderCreatedEvent: {}", orderEvent);
            // Gửi email thông báo đơn hàng
            emailService.sendEmailOrderSuccess(orderEvent);
            log.info("Order creation email sent successfully for order: {} to email: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail());
    }

    @KafkaListener(topics = "order-updated", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.OrderStatusChangedEvent"
    })
    public void handleOrderUpdate(OrderStatusChangedEvent orderEvent) {
        log.info("Received OrderStatusChangedEvent: {}", orderEvent);
        try {
            String contentText = String.format("Đơn hàng #%s của bạn đã được cập nhật trạng thái thành: %s.",
                    orderEvent.getOrderId(), orderEvent.getStatus());
            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "orderId", orderEvent.getOrderId(),
                    "status", orderEvent.getStatus(),
                    "link", "/account/orders/" + orderEvent.getOrderId()
            );
            notificationService.createNotification(orderEvent.getUserId(), NotificationType.NOTIFY, content);

            // Gửi email thông báo cập nhật trạng thái đơn hàng
            emailService.sendEmailOrderStatusUpdate(orderEvent);
            log.info("Order status update email sent successfully for order: {} to email: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send order status update email for order: {} to email: {}. Error: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail(), e.getMessage());
        }
    }

    @KafkaListener(topics = "user-cancel-order", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.OrderStatusChangedEvent"
    })
    public void handleUserCancelOrder(OrderStatusChangedEvent orderEvent) {
        log.info("Received OrderStatusChangedEvent on user-cancel-order: {}", orderEvent);
        try {
            String contentText = String.format("Đơn hàng #%s đã được hủy theo yêu cầu của bạn.", orderEvent.getOrderId());
            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "orderId", orderEvent.getOrderId(),
                    "status", "CANCELLED",
                    "link", "/account/orders/" + orderEvent.getOrderId()
            );
            notificationService.createNotification(orderEvent.getUserId(), NotificationType.NOTIFY, content);

            // Send email notification to the seller for cancellation
            emailService.sendEmailOrderCancelStatus(orderEvent);
            log.info("Order cancellation email sent successfully for order: {} to email: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancellation email for order: {} to email: {}. Error: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail(), e.getMessage());
        }
    }

    @KafkaListener(topics = "seller-verification", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.SellerVerificationEvent"
    })
    public void handleSellerVerification(SellerVerificationEvent event) {
        log.info("Received SellerVerificationEvent: {}", event);
        try {
            String contentText = String.format("Trạng thái xác thực gian hàng của bạn đã được cập nhật thành: %s.", event.getStatus());
            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "sellerId", event.getSellerId(),
                    "status", event.getStatus(),
                    "link", "/seller/profile"
            );
            notificationService.createNotification(event.getSellerId(), NotificationType.NOTIFY, content);

            // Send email notification for seller verification
            emailService.sendEmailSellerVerification(event);
            log.info("Seller verification email sent successfully for seller: {} to email: {}",
                    event.getSellerId(), event.getSellerEmail());
        } catch (Exception e) {
            log.error("Failed to send seller verification email for seller: {} to email: {}. Error: {}",
                    event.getSellerId(), event.getSellerEmail(), e.getMessage());
        }
    }
}