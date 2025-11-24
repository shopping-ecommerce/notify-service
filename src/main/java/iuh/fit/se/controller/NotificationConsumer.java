package iuh.fit.se.controller;

import iuh.fit.event.dto.*;
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
//        String contentText = String.format("Đơn hàng #%s của bạn đã được tạo thành công.", orderEvent.getOrderId());
//        Map<String, Object> content = Map.of(
//                "text", contentText,
//                "orderId", orderEvent.getOrderId(),
//                "link", "/user/orders/" + orderEvent.getOrderId()
//        );
//        notificationService.createNotification(orderEvent.getUserId(), NotificationType.NOTIFY, content);
        String contentTextSeller = String.format("Bạn có một đơn hàng mới #%s vừa được tạo.", orderEvent.getOrderId());
        Map<String, Object> contentOfSeller = Map.of(
                "text", contentTextSeller,
                "orderId", orderEvent.getOrderId(),
                "link", "/seller/orders/"+ orderEvent.getOrderId()
        );
        notificationService.createNotification(orderEvent.getSellerId(), NotificationType.NOTIFY, contentOfSeller);
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
            if( orderEvent.getStatus().equals("DELIVERED")) {
                String contentTextSeller = String.format("Đơn hàng #%s của bạn đã được giao thành công với số tiền: .", orderEvent.getOrderId());
                Map<String, Object> contentOfSeller = Map.of(
                        "text", contentTextSeller,
                        "orderId", orderEvent.getOrderId(),
                        "link", "/seller/orders/"+orderEvent.getOrderId()
                );
                notificationService.createNotification(orderEvent.getSellerId(), NotificationType.NOTIFY, contentOfSeller);
            }
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

            String contentTextSeller = String.format("Bạn có một đơn hàng mới #%s vừa bị hủy.", orderEvent.getOrderId());
            Map<String, Object> contentOfSeller = Map.of(
                    "text", contentTextSeller,
                    "orderId", orderEvent.getOrderId(),
                    "link", "/seller/orders/"+ orderEvent.getOrderId()
            );
            notificationService.createNotification(orderEvent.getSellerId(), NotificationType.NOTIFY, contentOfSeller);
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
                    "link", "/seller/home"
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

    @KafkaListener(topics = "product-invalid-notify", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.ProductInvalidNotify"
    })
    public void handleProductInvalid(ProductInvalidNotify productInvalidNotify) {
        log.info("Received ProductInvalid: {}", productInvalidNotify);
        try {
            log.info("Product invalid email sent successfully for product: {} to email: {}",
                    productInvalidNotify.getProductId(), productInvalidNotify.getEmail());
            emailService.sendEmailProductInvalid(productInvalidNotify);
        } catch (Exception e) {
            log.error("Failed to send product invalid email for product: {} to email: {}. Error: {}",
                    productInvalidNotify.getProductId(), productInvalidNotify.getEmail(), e.getMessage());
        }
    }

    @KafkaListener(topics = "policy-notification", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.PolicyEvent"
    })
    public void handlePolicyNotification(PolicyEvent policyEvent) {
        log.info("Received PolicyEvent: {}", policyEvent);
        try {
            // Gửi email cho từng người dùng trong danh sách
            for (String email : policyEvent.getEmails()) {
                emailService.sendEmailPolicyUpdate(policyEvent, email);
                log.info("Policy notification email sent successfully to: {}", email);
            }
        } catch (Exception e) {
            log.error("Failed to send policy notification emails. Error: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "seller-suspension", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.SellerSuspensionEvent"
    })
    public void handleSellerSuspension(SellerSuspensionEvent event) {
        log.info("Received SellerSuspensionEvent: {}", event);
        try {
            // Tạo thông báo in-app
            String contentText = String.format(
                    "Tài khoản bán hàng của bạn đã bị tạm khóa %d ngày do vi phạm: %s. " +
                            "Thời gian khóa đến: %s",
                    event.getSuspensionDays(),
                    event.getViolationType(),
                    event.getSuspensionEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "sellerId", event.getSellerId(),
                    "link", "/seller/violations"
            );
            notificationService.createNotification(event.getSellerId(), NotificationType.NOTIFY, content);

            // Gửi email thông báo
            emailService.sendEmailSellerSuspension(event);

            log.info("Seller suspension notification sent successfully to seller: {} at email: {}",
                    event.getSellerId(), event.getSellerEmail());
        } catch (Exception e) {
            log.error("Failed to send seller suspension notification for seller: {}. Error: {}",
                    event.getSellerId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "seller-warning", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.SellerWarningEvent"
    })
    public void handleSellerWarning(SellerWarningEvent event) {
        log.info("Received SellerWarningEvent: {}", event);
        try {
            // Tạo thông báo in-app
            String contentText = String.format(
                    "Cảnh báo: Bạn đã vi phạm quy định về %s. Đây là vi phạm lần %d. %s",
                    event.getViolationType(),
                    event.getViolationCount(),
                    event.getWarningMessage()
            );

            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "sellerId", event.getSellerId(),
                    "link", "/seller/violations"
            );
            notificationService.createNotification(event.getSellerId(), NotificationType.NOTIFY, content);
            // Gửi email cảnh báo
            emailService.sendEmailSellerWarning(event);

            log.info("Seller warning notification sent successfully to seller: {} at email: {}",
                    event.getSellerId(), event.getSellerEmail());
        } catch (Exception e) {
            log.error("Failed to send seller warning notification for seller: {}. Error: {}",
                    event.getSellerId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "policy-enforce-topic", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.PolicyEnforceEvent"
    })
    public void handlePolicyEnforce(iuh.fit.event.dto.PolicyEnforceEvent event) {
        log.info("Received PolicyEnforceEvent: {}", event);
        try {
            if (event.getEmails() != null && !event.getEmails().isEmpty()) {
                emailService.sendEmailPolicyEnforcementMinimal(event.getEmails());
                for (String em : event.getEmails()) {
                    log.info("Minimal policy enforcement email sent to: {}", em);
                }
            } else {
                log.warn("PolicyEnforceEvent has empty emails list.");
            }
        } catch (Exception e) {
            log.error("Failed to process PolicyEnforceEvent. Error: {}", e.getMessage());
        }
    }
    @KafkaListener(topics = "seller-unsuspension", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.SellerUnsuspensionEvent"
    })
    public void handleSellerUnsuspension(iuh.fit.event.dto.SellerUnsuspensionEvent event) {
        log.info("Received SellerUnsuspensionEvent: {}", event);
        try {
            // In-app notification
            String contentText = String.format(
                    "Tài khoản bán hàng của bạn đã được khôi phục lúc %s.",
                    event.getUnsuspendedAt() != null
                            ? event.getUnsuspendedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "N/A"
            );

            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "sellerId", event.getSellerId(),
                    "link", "/seller/home"
            );

            // Có thể dùng type SUCCESS/NOTIFY tùy enum của bạn
            notificationService.createNotification(event.getSellerId(), NotificationType.NOTIFY, content);

            // Email
            emailService.sendEmailSellerUnsuspension(event);

            log.info("Seller unsuspension notification sent to seller: {} at email: {}",
                    event.getSellerId(), event.getSellerEmail());
        } catch (Exception e) {
            log.error("Failed to process SellerUnsuspensionEvent for seller: {}. Error: {}",
                    event.getSellerId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "appeal-decision", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.AppealDecisionEvent"
    })
    public void handleAppealDecision(AppealDecisionEvent event) {
        log.info("Received AppealDecisionEvent: {}", event);
        try {
            // Tạo thông báo in-app
            String contentText = String.format(
                    "Khiếu nại #%s của bạn đã được xét duyệt: %s",
                    event.getAppealId(),
                    "APPROVED".equalsIgnoreCase(event.getStatus()) ? "Được chấp nhận" : "Bị từ chối"
            );

            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "appealId", event.getAppealId(),
                    "status", event.getStatus(),
                    "link", "/seller/appeals/" + event.getAppealId()
            );
            notificationService.createNotification(event.getSellerId(), NotificationType.NOTIFY, content);

            // Gửi email thông báo
            emailService.sendEmailAppealDecision(event);

            log.info("Appeal decision notification sent successfully for appeal: {} to seller: {} at email: {}",
                    event.getAppealId(), event.getSellerId(), event.getSellerEmail());
        } catch (Exception e) {
            log.error("Failed to send appeal decision notification for appeal: {}. Error: {}",
                    event.getAppealId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "user-banned", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=iuh.fit.event.dto.UserBanned"
    })
    public void handleUserBanned(UserBanned userBanned) {
        log.info("Received UserBanned event: {}", userBanned);
        try {
            // Tạo thông báo in-app
            String contentText = String.format(
                    "Tài khoản của bạn đã bị tạm khóa tính năng đặt hàng. Lý do: %s",
                    userBanned.getReason()
            );

            Map<String, Object> content = Map.of(
                    "text", contentText,
                    "userId", userBanned.getUserId(),
                    "link", "/account"
            );
            notificationService.createNotification(userBanned.getUserId(), NotificationType.NOTIFY, content);

            // Gửi email thông báo
            log.info("User banned notification sent successfully for user: {} to email: {}",
                    userBanned.getUserId());
        } catch (Exception e) {
            log.error("Failed to send user banned notification for user: {}. Error: {}",
                    userBanned.getUserId(), e.getMessage());
        }
    }
}