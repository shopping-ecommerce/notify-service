package iuh.fit.se.controller;

import iuh.fit.event.dto.NotificationEvent;
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
}