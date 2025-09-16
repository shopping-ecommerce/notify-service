package iuh.fit.se.controller;

import iuh.fit.event.dto.NotificationEvent;
import iuh.fit.event.dto.OrderCreatedEvent;
import iuh.fit.event.dto.OrderStatusChangedEvent;
import iuh.fit.se.dto.request.Recipient;
import iuh.fit.se.dto.request.SendEmailRequest;
import iuh.fit.se.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NotificationController {
    EmailService emailService;
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
            // Send email notification to the seller for cancellation
            emailService.sendEmailOrderCancelStatus(orderEvent);
            log.info("Order cancellation email sent successfully for order: {} to email: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancellation email for order: {} to email: {}. Error: {}",
                    orderEvent.getOrderId(), orderEvent.getUserEmail(), e.getMessage());
        }
    }
}