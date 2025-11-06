package iuh.fit.se.service.impl;

import feign.FeignException;
import iuh.fit.event.dto.*;
import iuh.fit.se.dto.request.Recipient;
import iuh.fit.se.dto.response.EmailReponse;
import iuh.fit.se.dto.request.EmailRequest;
import iuh.fit.se.dto.request.SendEmailRequest;
import iuh.fit.se.dto.request.Sender;
import iuh.fit.se.repository.httpclient.EmailClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailServiceImpl {
    final EmailClient emailClient;
//    Dotenv dotenv = Dotenv.load();
//    String apiKey = dotenv.get("API_KEY");
    @Value(value = "${brevo.api-key}")
    String apiKey;
    @Value(value = "${brevo.sender-email}")
    String email;
    public EmailReponse sendEmail(SendEmailRequest sendEmailRequest) {
        {
            String htmlContent = templateSendOTP(sendEmailRequest.getTo().getEmail(), sendEmailRequest.getHtmlContent());
            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(Sender.builder()
                            .name("SHOPPING")
                            .email(email)
                            .build())
                    .to(List.of(sendEmailRequest.getTo()))
                    .subject(sendEmailRequest.getSubject())
                    .htmlContent(htmlContent)
                    .build();
            try {
                return emailClient.sendEmail(apiKey, emailRequest);
            } catch (FeignException e) {
                throw new RuntimeException("Failed to send email" + e.contentUTF8());
            }
        }
    }

    public EmailReponse sendEmailSuccess(SendEmailRequest sendEmailRequest) {
        {
            String htmlContent = templateSuccessRegister(sendEmailRequest.getTo().getEmail(), sendEmailRequest.getHtmlContent());
            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(Sender.builder()
                            .name("SHOPPING")
                            .email(email)
                            .build())
                    .to(List.of(sendEmailRequest.getTo()))
                    .subject(sendEmailRequest.getSubject())
                    .htmlContent(htmlContent)
                    .build();
            try {
                return emailClient.sendEmail(apiKey, emailRequest);
            } catch (FeignException e) {
                throw new RuntimeException("Failed to send email" + e.contentUTF8());
            }
        }
    }

    public EmailReponse sendEmailOrderSuccess(OrderCreatedEvent orderCreatedEvent) {
        String htmlContent = templateOrderSuccess(orderCreatedEvent);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(orderCreatedEvent.getUserEmail())
                        .build()))
                .subject("Xác nhận đơn hàng #" + orderCreatedEvent.getOrderId())
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send order confirmation email: " + e.contentUTF8());
        }
    }


    public EmailReponse sendEmailOrderStatusUpdate(OrderStatusChangedEvent orderStatusChangedEvent) {
        String htmlContent = templateOrderStatusUpdate(orderStatusChangedEvent);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(orderStatusChangedEvent.getUserEmail())
                        .build()))
                .subject("Cập nhật trạng thái đơn hàng #" + orderStatusChangedEvent.getOrderId())
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send order status update email: " + e.contentUTF8());
        }
    }

    public EmailReponse sendEmailOrderCancelStatus(OrderStatusChangedEvent orderStatusChangedEvent) {
        String htmlContent = templateSellerOrderCancellation(orderStatusChangedEvent);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(orderStatusChangedEvent.getUserEmail())
                        .build()))
                .subject("Thông báo hủy đơn hàng #" + orderStatusChangedEvent.getOrderId())
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send order cancellation email: " + e.contentUTF8());
        }
    }

    public EmailReponse sendEmailSellerVerification(SellerVerificationEvent event) {
        String htmlContent = templateSellerVerification(event);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(event.getSellerEmail())
                        .build()))
                .subject("Kết quả xét duyệt hồ sơ bán hàng #" + event.getSellerId())
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send seller verification email: " + e.contentUTF8());
        }
    }

    public EmailReponse sendEmailProductInvalid(ProductInvalidNotify productInvalidNotify) {
        String htmlContent = templateProductInvalid(productInvalidNotify);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(productInvalidNotify.getEmail())
                        .build()))
                .subject("Thông báo sản phẩm không hợp lệ - " + productInvalidNotify.getProductName())
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send product invalid email: " + e.contentUTF8());
        }
    }private String templateProductInvalid(ProductInvalidNotify notify) {
        String statusColor = "#ef4444"; // Red for invalid
        String statusIcon = "⚠️";
        String statusMessage = "Sản phẩm của bạn đã bị đánh dấu là không hợp lệ và đã bị gỡ khỏi cửa hàng. Vui lòng xem lý do bên dưới.";

        // Build rejection reason section
        String rejectionReasonHtml = "";
        if (notify.getReason() != null && !notify.getReason().trim().isEmpty()) {
            rejectionReasonHtml =
                    "      <div style=\"background-color: #fef2f2; border-left: 3px solid #ef4444; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                            "        <h4 style=\"color: #dc2626; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lý do không hợp lệ</h4>" +
                            "        <p style=\"color: #991b1b; margin: 0; line-height: 1.5; font-size: 14px;\">" + notify.getReason() + "</p>" +
                            "      </div>";
        }

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Thông báo sản phẩm không hợp lệ</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +

                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Thông báo sản phẩm không hợp lệ</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Sản phẩm #" + notify.getProductId() + "</p>" +
                "      </div>" +
                "    </div>" +

                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +

                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Kính gửi Người bán,</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">" + statusMessage + "</p>" +
                "      </div>" +

                "      <!-- Status -->" +
                "      <div style=\"background-color: " + statusColor + "; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: center;\">" +
                "          <span style=\"margin-right: 8px; font-size: 16px;\">" + statusIcon + "</span>" +
                "          <span style=\"color: #ffffff; font-weight: 500; font-size: 14px;\">Không hợp lệ</span>" +
                "        </div>" +
                "      </div>" +

                "      <!-- Product Info -->" +
                "      <div style=\"border: 1px solid #e9ecef; border-radius: 6px; padding: 20px; margin: 24px 0; background-color: #f8f9fa;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 12px;\">Thông tin sản phẩm</h3>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Mã sản phẩm:</span>" +
                "          <span style=\"color: #212529; font-weight: 500; font-size: 14px;\">" + notify.getProductId() + "</span>" +
                "        </div>" +
                "        <div style=\"display: flex; justify-content: space-between;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Tên sản phẩm:</span>" +
                "          <span style=\"color: #212529; font-weight: 500; font-size: 14px;\">" + notify.getProductName() + "</span>" +
                "        </div>" +
                "      </div>" +

                rejectionReasonHtml +

                "      <!-- Action Steps -->" +
                "      <div style=\"background-color: #fff7ed; border: 1px solid #fed7aa; padding: 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"color: #ea580c; font-size: 14px; font-weight: 500; margin: 0 0 12px; text-transform: uppercase; letter-spacing: 0.5px;\">Các bước tiếp theo</h4>" +
                "        <ul style=\"color: #9a3412; margin: 0; padding-left: 20px; line-height: 1.6; font-size: 14px;\">" +
                "          <li style=\"margin-bottom: 8px;\">Xem xét lại sản phẩm và lý do không hợp lệ</li>" +
                "          <li style=\"margin-bottom: 8px;\">Chỉnh sửa thông tin sản phẩm theo yêu cầu</li>" +
                "          <li style=\"margin-bottom: 8px;\">Đăng lại sản phẩm để được xét duyệt</li>" +
                "          <li>Liên hệ hỗ trợ nếu cần giải thích thêm</li>" +
                "        </ul>" +
                "      </div>" +

                "      <!-- Action Button -->" +
                "      <div style=\"text-align: center; margin: 40px 0 32px;\">" +
                "        <a href=\"http://localhost:3000/seller/products/" + notify.getProductId() + "\" " +
                "           style=\"display: inline-block; background-color: #212529; color: #ffffff; " +
                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                "           font-size: 14px; transition: background-color 0.2s ease;\">" +
                "          Xem sản phẩm" +
                "        </a>" +
                "      </div>" +

                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
    private String templateSellerVerification(SellerVerificationEvent event) {
        String statusText = event.getStatus().equalsIgnoreCase("APPROVED") ? "Đã được duyệt" : "Bị từ chối";
        String statusColor = event.getStatus().equalsIgnoreCase("APPROVED") ? "#22c55e" : "#ef4444"; // Green or Red
        String statusIcon = event.getStatus().equalsIgnoreCase("APPROVED") ? "✅" : "❌";
        String statusMessage = event.getStatus().equalsIgnoreCase("APPROVED")
                ? "Chúc mừng! Hồ sơ bán hàng của bạn đã được duyệt. Bạn có thể bắt đầu bán hàng ngay bây giờ."
                : "Rất tiếc, hồ sơ bán hàng của bạn đã bị từ chối. Vui lòng xem lý do bên dưới.";

        // Build rejection reason section if applicable
        String rejectionReasonHtml = "";
        if ("REJECTED".equalsIgnoreCase(event.getStatus()) && event.getReason() != null && !event.getReason().trim().isEmpty()) {
            rejectionReasonHtml =
                    "      <div style=\"background-color: #fef2f2; border-left: 3px solid #ef4444; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                            "        <h4 style=\"color: #dc2626; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lý do từ chối</h4>" +
                            "        <p style=\"color: #991b1b; margin: 0; line-height: 1.5; font-size: 14px;\">" + event.getReason() + "</p>" +
                            "      </div>";
        }

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Kết quả xét duyệt hồ sơ bán hàng #" + event.getSellerId() + "</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +
                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Kết quả xét duyệt hồ sơ</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Hồ sơ #" + event.getSellerId() + "</p>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +
                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Kính gửi " + event.getSellerEmail() + ",</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">" + statusMessage + "</p>" +
                "      </div>" +
                "      <!-- Status -->" +
                "      <div style=\"background-color: " + statusColor + "; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: center;\">" +
                "          <span style=\"margin-right: 8px; font-size: 16px;\">" + statusIcon + "</span>" +
                "          <span style=\"color: #ffffff; font-weight: 500; font-size: 14px;\">" + statusText + "</span>" +
                "        </div>" +
                "      </div>" +
                rejectionReasonHtml +
                "      <!-- Action Button -->" +
                (event.getStatus().equalsIgnoreCase("APPROVED") ?
                        "      <div style=\"text-align: center; margin: 40px 0 32px;\">" +
                                "        <a href=\"http://localhost:3000/seller/dashboard\" " +
                                "           style=\"display: inline-block; background-color: #212529; color: #ffffff; " +
                                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                                "           font-size: 14px; transition: background-color 0.2s ease;\">" +
                                "          Truy cập bảng điều khiển bán hàng" +
                                "        </a>" +
                                "      </div>" : "") +
                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
    private static String renderOptionsHtml(Map<String, String> opts) {
        if (opts == null || opts.isEmpty()) {
            return "<div style=\"font-size: 13px; color: #6c757d;\">Options: N/A</div>";
        }
        StringBuilder sb = new StringBuilder();
        for (var e : opts.entrySet()) {
            String k = StringEscapeUtils.escapeHtml4(e.getKey());
            String v = StringEscapeUtils.escapeHtml4(String.valueOf(e.getValue()));
            sb.append("<div style=\"font-size: 13px; color: #6c757d;\">")
                    .append(k).append(": ").append(v)
                    .append("</div>");
        }
        return sb.toString();
    }
    private String templateSellerOrderCancellation(OrderStatusChangedEvent order) {
        String statusText = "Đã hủy";
        String statusColor = "#ef4444"; // Red for CANCELLED
        String statusIcon = "❌";
        String statusMessage = "Đơn hàng của bạn đã bị khách hàng hủy. Vui lòng xem chi tiết bên dưới.";

        // Lý do hủy
        String cancellationReasonHtml = "";
        if (order.getReason() != null && !order.getReason().trim().isEmpty()) {
            cancellationReasonHtml =
                    "      <div style=\"background-color: #fef2f2; border-left: 3px solid #ef4444; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                            "        <h4 style=\"color: #dc2626; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lý do hủy đơn</h4>" +
                            "        <p style=\"color: #991b1b; margin: 0; line-height: 1.5; font-size: 14px;\">" + order.getReason() + "</p>" +
                            "      </div>";
        }

        // Bảng sản phẩm
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItemPayload item : order.getItems()) {
        String optionsHtml = renderOptionsHtml(item.getOptions());
                itemsHtml.append(String.format(
                        "<tr>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; vertical-align: top;\">" +
                                "    <div style=\"display: flex; align-items: center;\">" +
                                "      <div style=\"width: 40px; height: 40px; background-color: #f8f9fa; border-radius: 6px; margin-right: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #e9ecef;\">" +
                                "        <span style=\"color: #6c757d; font-size: 16px;\">📦</span>" +
                                "      </div>" +
                                "      <div>" +
                                "        <div style=\"font-weight: 500; color: #212529; font-size: 15px; margin-bottom: 2px;\">%s</div>" +
                                "        <div style=\"font-size: 13px; color: #6c757d;\">%s</div>" +
                                "      </div>" +
                                "    </div>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                                "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                                "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%s</span>" +
                                "  </td>" +
                                "</tr>",
                        item.getProductName(),
                        optionsHtml,
                        item.getQuantity(),
                        formatCurrency(item.getSubTotal())
                ));
            }
        }

        // --- BẮT CHƯỚC PHẦN GIẢM GIÁ (y như template thứ hai) ---
        String discountHtml = "";
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml =
                    "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                            "          <span style=\"color: #6c757d; font-size: 14px;\">Giảm giá:</span>" +
                            "          <span style=\"color: #dc2626; font-weight: 500; font-size: 14px;\">-" + formatCurrency(order.getDiscountAmount()) + "</span>" +
                            "        </div>";
        }
        // ---------------------------------------------------------

        // Tính/hiển thị các khoản tiền: dùng dữ liệu từ order
        String subtotalStr     = formatCurrency(order.getSubtotal());     // tạm tính
        String shippingFeeStr  = formatCurrency(order.getShippingFee());  // phí vận chuyển
        String totalAmountStr  = formatCurrency(order.getTotalAmount());  // tổng cộng

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Thông báo hủy đơn hàng #" + order.getOrderId() + "</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +
                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Thông báo hủy đơn hàng</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Đơn hàng #" + order.getOrderId() + "</p>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +
                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Kính gửi Người bán,</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">" + statusMessage + "</p>" +
                "      </div>" +
                "      <!-- Order Status -->" +
                "      <div style=\"background-color: " + statusColor + "; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: center;\">" +
                "          <span style=\"margin-right: 8px; font-size: 16px;\">" + statusIcon + "</span>" +
                "          <span style=\"color: #ffffff; font-weight: 500; font-size: 14px;\">" + statusText + "</span>" +
                "        </div>" +
                "      </div>" +
                cancellationReasonHtml +
                "      <!-- Order Items -->" +
                "      <div style=\"margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Chi tiết đơn hàng</h3>" +
                "        <div style=\"border: 1px solid #e9ecef; border-radius: 6px; overflow: hidden;\">" +
                "          <table style=\"width: 100%; border-collapse: collapse; background-color: #ffffff;\">" +
                "            <thead>" +
                "              <tr style=\"background-color: #f8f9fa;\">" +
                "                <th style=\"padding: 12px 16px; text-align: left; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Sản phẩm</th>" +
                "                <th style=\"padding: 12px 16px; text-align: center; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Số lượng</th>" +
                "                <th style=\"padding: 12px 16px; text-align: right; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Giá</th>" +
                "              </tr>" +
                "            </thead>" +
                "            <tbody>" +
                itemsHtml.toString() +
                "            </tbody>" +
                "          </table>" +
                "        </div>" +
                "      </div>" +
                "      <!-- Order Summary -->" +
                "      <div style=\"background-color: #f8f9fa; padding: 24px; border-radius: 6px; margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Tóm tắt đơn hàng</h3>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Tạm tính:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + subtotalStr + "</span>" +
                "        </div>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + shippingFeeStr + "</span>" +
                "        </div>" +
                discountHtml +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + totalAmountStr + "</span>" +
                "        </div>" +
                "      </div>" +
                "      <!-- Shipping Address -->" +
                "      <div style=\"border-left: 3px solid #dee2e6; padding: 16px 20px; background-color: #f8f9fa; margin: 24px 0;\">" +
                "        <h4 style=\"color: #212529; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Địa chỉ giao hàng</h4>" +
                "        <p style=\"color: #495057; margin: 0; font-size: 14px; line-height: 1.5;\">" + (order.getShippingAddress() != null ? order.getShippingAddress() : "Không có thông tin") + "</p>" +
                "      </div>" +
                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
    private String templateOrderStatusUpdate(OrderStatusChangedEvent order) {
        String statusText = getStatusText(order.getStatus());
        String statusColor = getStatusColor(order.getStatus());
        String statusIcon = getStatusIcon(order.getStatus());
        String statusMessage = getStatusMessage(order.getStatus());

        // Build cancellation reason section if order is cancelled and reason exists
        String cancellationReasonHtml = "";
        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) && order.getReason() != null && !order.getReason().trim().isEmpty()) {
            cancellationReasonHtml =
                    "      <div style=\"background-color: #fef2f2; border-left: 3px solid #ef4444; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                            "        <h4 style=\"color: #dc2626; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lý do hủy đơn</h4>" +
                            "        <p style=\"color: #991b1b; margin: 0; line-height: 1.5; font-size: 14px;\">" + order.getReason() + "</p>" +
                            "      </div>";
        }

        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItemPayload item : order.getItems()) {
                String optionsHtml = renderOptionsHtml(item.getOptions());
                itemsHtml.append(String.format(
                        "<tr>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; vertical-align: top;\">" +
                                "    <div style=\"display: flex; align-items: center;\">" +
                                "      <div style=\"width: 40px; height: 40px; background-color: #f8f9fa; border-radius: 6px; margin-right: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #e9ecef;\">" +
                                "        <span style=\"color: #6c757d; font-size: 16px;\">📦</span>" +
                                "      </div>" +
                                "      <div>" +
                                "        <div style=\"font-weight: 500; color: #212529; font-size: 15px; margin-bottom: 2px;\">%s</div>" +
                                "        <div style=\"font-size: 13px; color: #6c757d;\">%s</div>" +
                                "      </div>" +
                                "    </div>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                                "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                                "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%s</span>" +
                                "  </td>" +
                                "</tr>",
                        item.getProductName(),
                        optionsHtml ,
                        item.getQuantity(),
                        formatCurrency(item.getSubTotal())
                ));
            }
        }

        // Tạo HTML cho phần giảm giá
        String discountHtml = "";
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml =
                    "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                            "          <span style=\"color: #6c757d; font-size: 14px;\">Giảm giá:</span>" +
                            "          <span style=\"color: #dc2626; font-weight: 500; font-size: 14px;\">-" + formatCurrency(order.getDiscountAmount()) + "</span>" +
                            "        </div>";
        }

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Cập nhật đơn hàng #" + order.getOrderId() + "</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +

                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Cập nhật đơn hàng</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Đơn hàng #" + order.getOrderId() + "</p>" +
                "      </div>" +
                "    </div>" +

                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +

                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Xin chào " + (order.getRecipientName() != null ? order.getRecipientName() : "Khách hàng") + ",</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">" + statusMessage + "</p>" +
                "      </div>" +

                "      <!-- Order Status -->" +
                "      <div style=\"background-color: " + statusColor + "; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: center;\">" +
                "          <span style=\"margin-right: 8px; font-size: 16px;\">" + statusIcon + "</span>" +
                "          <span style=\"color: #ffffff; font-weight: 500; font-size: 14px;\">" + statusText + "</span>" +
                "        </div>" +
                "      </div>" +

                cancellationReasonHtml +

                "      <!-- Order Items -->" +
                "      <div style=\"margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Chi tiết đơn hàng</h3>" +
                "        <div style=\"border: 1px solid #e9ecef; border-radius: 6px; overflow: hidden;\">" +
                "          <table style=\"width: 100%; border-collapse: collapse; background-color: #ffffff;\">" +
                "            <thead>" +
                "              <tr style=\"background-color: #f8f9fa;\">" +
                "                <th style=\"padding: 12px 16px; text-align: left; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Sản phẩm</th>" +
                "                <th style=\"padding: 12px 16px; text-align: center; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Số lượng</th>" +
                "                <th style=\"padding: 12px 16px; text-align: right; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Giá</th>" +
                "              </tr>" +
                "            </thead>" +
                "            <tbody>" +
                itemsHtml.toString() +
                "            </tbody>" +
                "          </table>" +
                "        </div>" +
                "      </div>" +

                "      <!-- Order Summary -->" +
                "      <div style=\"background-color: #f8f9fa; padding: 24px; border-radius: 6px; margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Tóm tắt đơn hàng</h3>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Tạm tính:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + formatCurrency(order.getSubtotal()) + "</span>" +
                "        </div>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + formatCurrency(order.getShippingFee()) + "</span>" +
                "        </div>" +
                discountHtml +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + formatCurrency(order.getTotalAmount()) + "</span>" +
                "        </div>" +
                "      </div>" +

                "      <!-- Shipping Address -->" +
                "      <div style=\"border-left: 3px solid #dee2e6; padding: 16px 20px; background-color: #f8f9fa; margin: 24px 0;\">" +
                "        <h4 style=\"color: #212529; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Địa chỉ giao hàng</h4>" +
                "        <p style=\"color: #495057; margin: 0; font-size: 14px; line-height: 1.5;\">" + (order.getShippingAddress() != null ? order.getShippingAddress() : "Không có thông tin") + "</p>" +
                "      </div>" +

                "      <!-- Action Button -->" +
                (!("CANCELLED".equalsIgnoreCase(order.getStatus())) ?
                        "      <!-- Action Button -->" +
                                "      <div style=\"text-align: center; margin: 40px 0 32px;\">" +
                                "        <a href=\"http://localhost:3000/orders/" + order.getOrderId() + "\" " +
                                "           style=\"display: inline-block; background-color: #212529; color: #ffffff; " +
                                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                                "           font-size: 14px; transition: background-color 0.2s ease;\">" +
                                "          Theo dõi đơn hàng" +
                                "        </a>" +
                                "      </div>" : ""
                ) +

                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    // Cũng cần cập nhật lại các hàm helper để có màu sắc phù hợp với thiết kế mới
    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "#f59e0b"; // Amber
            case "CONFIRMED":
                return "#10b981"; // Emerald
            case "SHIPPED":
                return "#8b5cf6"; // Violet
            case "DELIVERED":
                return "#22c55e"; // Green
            case "CANCELLED":
                return "#ef4444"; // Red
            default:
                return "#6b7280"; // Gray
        }
    }

    private String getStatusMessage(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Đơn hàng của bạn đang chờ được xử lý từ SHOPPING.";
            case "CONFIRMED":
                return "Tuyệt vời! Đơn hàng của bạn đã được xác nhận và sẽ sớm được xử lý.";
            case "PROCESSING":
                return "Đơn hàng của bạn đang được chuẩn bị. Chúng tôi sẽ sớm gửi hàng cho bạn.";
            case "SHIPPED":
                return "Đơn hàng đã được gửi đi. Hãy theo dõi quá trình vận chuyển nhé.";
            case "DELIVERED":
                return "Tuyệt vời! Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua sắm tại SHOPPING.";
            case "CANCELLED":
                return "Đơn hàng của bạn đã được hủy. Nếu có thắc mắc, vui lòng liên hệ với chúng tôi.";
            case "RETURNED":
                return "Đơn hàng của bạn đã được trả lại. Chúng tôi sẽ xử lý yêu cầu hoàn tiền sớm nhất có thể.";
            case "REFUNDED":
                return "Tiền đã được hoàn lại vào tài khoản của bạn. Cảm ơn bạn đã thông cảm.";
            default:
                return "Đơn hàng của bạn có cập nhật mới từ SHOPPING.";
        }
    }


    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xử lý";
            case "SHIPPED":
                return "Đang giao hàng";
            case "DELIVERED":
                return "Đã giao hàng";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return "Cập nhật trạng thái";
        }
    }

    private String getStatusIcon(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "⏳";
            case "CONFIRMED":
                return "✅";
            case "SHIPPED":
                return "🚚";
            case "DELIVERED":
                return "🎉";
            case "CANCELLED":
                return "❌";
            default:
                return "📋";
        }
    }


    private String templateSendOTP(String name, String otp) {
        String verifyUrl = String.format(
                "http://localhost:8888/shopping/api/authentication/verifyFromEmail?email=%s",
                URLEncoder.encode(name, StandardCharsets.UTF_8)
        );
        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <title>Xác thực OTP</title>" +
                "</head>" +
                "<body style=\"font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;\">" +
                "  <div style=\"max-width: 600px; margin: auto; background-color: #ffffff; padding: 20px; border: 1px solid #ddd;\">" +

                // Logo thu nhỏ
                "    <div style=\"text-align: center; margin-bottom: 20px;\">" +
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"Logo\" style=\"max-width: 100px; height: auto; border-radius: 10px;\"/>" +
                "    </div>" +

                // Nội dung chính
                "    <h2 style=\"color: #333; text-align: center;\">Xác thực tài khoản SHOPPING</h2>" +
                "    <p>Xin chào <strong>" + (name != null ? name : "Khách hàng") + "</strong>,</p>" +
                "    <p style=\"text-align: center;\">Mã OTP của bạn là:</p>" +
                "    <div style=\"text-align: center; margin: 20px 0;\">" +
                "      <span style=\"font-size: 32px; font-weight: bold; color: #2c3e50; background-color: #f0f0f0; padding: 15px 30px; display: inline-block; border-radius: 8px;\">" + otp + "</span>" +
                "    </div>" +
                "    <p style=\"text-align: center;\">Mã có hiệu lực trong <strong>1 phút</strong>.</p>" +
                "    <p style=\"text-align: center;\">" +
                "       <a href=\"" + verifyUrl + "\" style=\"background-color: #3498db; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 5px;\">Xác thực ngay</a>" +
                "    <p style=\"text-align: center; margin-top: 30px; font-size: 14px;\">Nếu bạn không yêu cầu mã này, vui lòng liên hệ <a href=\"mailto:thinh183tt@gmail.com\">support@shopping.com</a>.</p>" +

                // Footer giống Riot
                "    <hr style=\"margin: 40px 0;\">" +
                "    <div style=\"text-align: center;\">" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Facebook\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Instagram\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"YouTube\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733579.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Twitter\"></a>" +
                "    </div>" +
                "    <p style=\"text-align: center; font-size: 13px; color: #999; margin-top: 20px;\">" +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">CHÍNH SÁCH QUYỀN RIÊNG TƯ</a> • " +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">HỖ TRỢ</a> • " +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">ĐIỀU KHOẢN SỬ DỤNG</a>" +
                "    </p>" +
                "    <p style=\"text-align: center; font-size: 12px; color: #aaa; margin-top: 10px;\">" +
                "      © 2025 SHOPPING. Mọi quyền được bảo lưu." +
                "    </p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String templateSuccessRegister(String name, String email) {
        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <title>Chúc mừng đăng ký tài khoản</title>" +
                "</head>" +
                "<body style=\"font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;\">" +
                "  <div style=\"max-width: 600px; margin: auto; background-color: #ffffff; padding: 20px; border: 1px solid #ddd;\">" +
                // Logo
                "    <div style=\"text-align: center; margin-bottom: 20px;\">" +
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"Logo\" style=\"max-width: 100px; height: auto; border-radius: 10px;\"/>" +
                "    </div>" +
                // Nội dung chính
                "    <h2 style=\"color: #333; text-align: center;\">Chúc mừng bạn đã đăng ký thành công!</h2>" +
                "    <p>Xin chào <strong>" + (email != null ? email : "Khách hàng") + "</strong>,</p>" +
                "    <p style=\"text-align: center;\">Tài khoản của bạn với email <strong>" + name + "</strong> đã được tạo thành công.</p>" +
                "    <p style=\"text-align: center;\">Bây giờ bạn có thể đăng nhập và bắt đầu trải nghiệm các dịch vụ tuyệt vời của SHOPPING!</p>" +
                "    <p style=\"text-align: center; margin: 20px 0;\">" +
                "      <a href=\"http://localhost:3000/\" style=\"background-color: #3498db; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 5px;\">Đăng nhập ngay</a>" +
                "    </p>" +
                "    <p style=\"text-align: center; margin-top: 30px; font-size: 14px;\">Nếu bạn gặp bất kỳ vấn đề nào, vui lòng liên hệ <a href=\"mailto:thinh183tt@gmail.com\">support@shopping.com</a>.</p>" +
                // Footer giống Riot
                "    <hr style=\"margin: 40px 0;\">" +
                "    <div style=\"text-align: center;\">" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Facebook\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Instagram\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"YouTube\"></a>" +
                "      <a href=\"#\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733579.png\" width=\"24\" style=\"margin: 0 8px;\" alt=\"Twitter\"></a>" +
                "    </div>" +
                "    <p style=\"text-align: center; font-size: 13px; color: #999; margin-top: 20px;\">" +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">CHÍNH SÁCH QUYỀN RIÊNG TƯ</a> • " +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">HỖ TRỢ</a> • " +
                "      <a href=\"#\" style=\"margin: 0 5px; color: #666; text-decoration: none;\">ĐIỀU KHOẢN SỬ DỤNG</a>" +
                "    </p>" +
                "    <p style=\"text-align: center; font-size: 12px; color: #aaa; margin-top: 10px;\">" +
                "      © 2025 SHOPPING. Mọi quyền được bảo lưu." +
                "    </p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String templateOrderSuccess(OrderCreatedEvent order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItemPayload item : order.getItems()) {
            itemsHtml.append(String.format(
                    "<tr>" +
                            "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; vertical-align: top;\">" +
                            "    <div style=\"display: flex; align-items: center;\">" +
                            "      <div style=\"width: 40px; height: 40px; background-color: #f8f9fa; border-radius: 6px; margin-right: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #e9ecef;\">" +
                            "        <span style=\"color: #6c757d; font-size: 16px;\">📦</span>" +
                            "      </div>" +
                            "      <div>" +
                            "        <div style=\"font-weight: 500; color: #212529; font-size: 15px; margin-bottom: 2px;\">%s</div>" +
                            "        <div style=\"font-size: 13px; color: #6c757d;\">%s</div>" +
                            "      </div>" +
                            "    </div>" +
                            "  </td>" +
                            "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                            "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                            "  </td>" +
                            "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                            "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%s</span>" +
                            "  </td>" +
                            "</tr>",
                    item.getProductName(),
                    item.getOptions() != null ? item.getOptions() : "N/A",
                    item.getQuantity(),
                    formatCurrency(item.getSubTotal())
            ));
        }

        // Tạo HTML cho phần giảm giá (chỉ hiển thị nếu có giảm giá)
        String discountHtml = "";
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml =
                    "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                            "          <span style=\"color: #6c757d; font-size: 14px;\">Giảm giá:</span>" +
                            "          <span style=\"color: #dc2626; font-weight: 500; font-size: 14px;\">-" + formatCurrency(order.getDiscountAmount()) + "</span>" +
                            "        </div>";
        }

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Xác nhận đơn hàng #" + order.getOrderId() + "</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +
                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Đặt hàng thành công</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Đơn hàng #" + order.getOrderId() + "</p>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +
                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Xin chào " + (order.getRecipientName() != null ? order.getRecipientName() : "Khách hàng") + ",</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">Cảm ơn bạn đã đặt hàng tại SHOPPING. Đơn hàng của bạn đã được xác nhận và đang được chuẩn bị.</p>" +
                "      </div>" +
                "      <!-- Status -->" +
                "      <div style=\"background-color: #f8fff4; border: 1px solid #d1f2a7; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: center;\">" +
                "          <span style=\"color: #22c55e; margin-right: 8px; font-size: 16px;\">✓</span>" +
                "          <span style=\"color: #15803d; font-weight: 500; font-size: 14px;\">Đã xác nhận</span>" +
                "        </div>" +
                "      </div>" +
                "      <!-- Order Items -->" +
                "      <div style=\"margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Chi tiết đơn hàng</h3>" +
                "        <div style=\"border: 1px solid #e9ecef; border-radius: 6px; overflow: hidden;\">" +
                "          <table style=\"width: 100%; border-collapse: collapse; background-color: #ffffff;\">" +
                "            <thead>" +
                "              <tr style=\"background-color: #f8f9fa;\">" +
                "                <th style=\"padding: 12px 16px; text-align: left; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Sản phẩm</th>" +
                "                <th style=\"padding: 12px 16px; text-align: center; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Số lượng</th>" +
                "                <th style=\"padding: 12px 16px; text-align: right; font-weight: 500; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;\">Giá</th>" +
                "              </tr>" +
                "            </thead>" +
                "            <tbody>" +
                itemsHtml.toString() +
                "            </tbody>" +
                "          </table>" +
                "        </div>" +
                "      </div>" +
                "      <!-- Order Summary -->" +
                "      <div style=\"background-color: #f8f9fa; padding: 24px; border-radius: 6px; margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Tóm tắt đơn hàng</h3>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Tạm tính:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + formatCurrency(order.getSubtotal()) + "</span>" +
                "        </div>" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">" +
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #495057; font-weight: 500; font-size: 14px;\">" + formatCurrency(order.getShippingFee()) + "</span>" +
                "        </div>" +
                discountHtml +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + formatCurrency(order.getTotalAmount()) + "</span>" +
                "        </div>" +
                "      </div>" +
                "      <!-- Shipping Address -->" +
                "      <div style=\"border-left: 3px solid #dee2e6; padding: 16px 20px; background-color: #f8f9fa; margin: 24px 0;\">" +
                "        <h4 style=\"color: #212529; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Địa chỉ giao hàng</h4>" +
                "        <p style=\"color: #495057; margin: 0; font-size: 14px; line-height: 1.5;\">" + order.getShippingAddress() + "</p>" +
                "      </div>" +
                "      <!-- Action Button -->" +
                "      <div style=\"text-align: center; margin: 40px 0 32px;\">" +
                "        <a href=\"http://localhost:3000/orders/" + order.getOrderId() + "\" " +
                "           style=\"display: inline-block; background-color: #212529; color: #ffffff; " +
                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                "           font-size: 14px; transition: background-color 0.2s ease;\">" +
                "          Theo dõi đơn hàng" +
                "        </a>" +
                "      </div>" +
                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +
                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }


    // Thêm method helper để format tiền
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0₫";
        return String.format("%,.0f₫", amount);
    }
    // Thêm method này vào class EmailServiceImpl

    public EmailReponse sendEmailPolicyUpdate(PolicyEvent policyEvent, String recipientEmail) {
        String htmlContent = templatePolicyUpdate(policyEvent);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("SHOPPING")
                        .email(email)
                        .build())
                .to(List.of(Recipient.builder()
                        .email(recipientEmail)
                        .build()))
                .subject("Thông báo cập nhật Chính sách & Điều khoản")
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send policy update email: " + e.contentUTF8());
        }
    }

    private String templatePolicyUpdate(PolicyEvent policy) {
        String formattedDate = policy.getStartDate() != null
                ? policy.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Thông báo cập nhật Chính sách</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 0; }" +
                "    @media only screen and (max-width: 600px) {" +
                "      .container { width: 100% !important; margin: 10px !important; }" +
                "      .content { padding: 20px !important; }" +
                "      .header { padding: 30px 20px !important; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; line-height: 1.6;\">" +
                "  <div class=\"container\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\">" +

                "    <!-- Header -->" +
                "    <div class=\"header\" style=\"background-color: #ffffff; padding: 40px 32px 30px; border-bottom: 1px solid #f0f0f0;\">" +
                "      <div style=\"text-align: center;\">" +
                "        <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width: 60px; height: auto; margin-bottom: 20px;\"/>" +
                "        <h1 style=\"margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #212529; letter-spacing: -0.25px;\">Cập nhật Chính sách & Điều khoản</h1>" +
                "        <p style=\"margin: 0; font-size: 15px; color: #6c757d;\">Thông báo quan trọng</p>" +
                "      </div>" +
                "    </div>" +

                "    <!-- Content -->" +
                "    <div class=\"content\" style=\"padding: 32px;\">" +

                "      <!-- Greeting -->" +
                "      <div style=\"margin-bottom: 32px;\">" +
                "        <h2 style=\"color: #212529; margin: 0 0 8px; font-size: 18px; font-weight: 500;\">Kính gửi Quý khách hàng,</h2>" +
                "        <p style=\"color: #6c757d; font-size: 15px; margin: 0; line-height: 1.5;\">" +
                "          Chúng tôi xin thông báo về việc cập nhật Chính sách và Điều khoản sử dụng của SHOPPING. " +
                "          Những thay đổi này sẽ có hiệu lực từ ngày <strong>" + formattedDate + "</strong>." +
                "        </p>" +
                "      </div>" +

                "      <!-- Important Notice -->" +
                "      <div style=\"background-color: #fff7ed; border: 1px solid #fed7aa; padding: 20px; border-radius: 6px; margin: 24px 0;\">" +
                "        <div style=\"display: flex; align-items: flex-start;\">" +
                "          <span style=\"color: #ea580c; margin-right: 12px; font-size: 20px;\">⚠️</span>" +
                "          <div>" +
                "            <h4 style=\"color: #ea580c; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lưu ý quan trọng</h4>" +
                "            <p style=\"color: #9a3412; margin: 0; line-height: 1.5; font-size: 14px;\">" +
                "              Việc đồng ý chính sách của chúng tôi sau ngày <strong>" + formattedDate + "</strong> " +
                "              đồng nghĩa với việc bạn chấp nhận các điều khoản và chính sách mới. Ngược lại chấm dứt hợp tác. Bạn có 7 - 30 ngày để quyết định. Xin chân thành cảm ơn!" +
                "            </p>" +
                "          </div>" +
                "        </div>" +
                "      </div>" +

                "      <!-- Effective Date -->" +
                "      <div style=\"border-left: 3px solid #3b82f6; padding: 16px 20px; background-color: #eff6ff; margin: 24px 0;\">" +
                "        <h4 style=\"color: #1e40af; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Ngày hiệu lực</h4>" +
                "        <p style=\"color: #1e3a8a; margin: 0; font-size: 16px; font-weight: 600;\">" + formattedDate + "</p>" +
                "      </div>" +

                "      <!-- What Changed -->" +
                "      <div style=\"margin: 32px 0;\">" +
                "        <h3 style=\"color: #212529; font-size: 16px; font-weight: 500; margin: 0 0 16px;\">Nội dung thay đổi chính</h3>" +
                "        <ul style=\"color: #495057; margin: 0; padding-left: 20px; line-height: 1.8; font-size: 14px;\">" +
                "          <li style=\"margin-bottom: 8px;\">Điều chỉnh điều khoản</li>" +
                "        </ul>" +
                "      </div>" +

                "      <!-- Action Buttons -->" +
                "      <div style=\"text-align: center; margin: 40px 0 32px;\">" +
                (policy.getPdfUrl() != null && !policy.getPdfUrl().trim().isEmpty() ?
                        "        <a href=\"" + policy.getPdfUrl() + "\" " +
                                "           style=\"display: inline-block; background-color: #dc2626; color: #ffffff; " +
                                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                                "           font-size: 14px; margin: 0 8px 12px; transition: background-color 0.2s ease;\">" +
                                "          📄 Tải file PDF" +
                                "        </a>" : "") +
                "        <a href=\"http://localhost:3000/policies\" " +
                "           style=\"display: inline-block; background-color: #212529; color: #ffffff; " +
                "           padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 500; " +
                "           font-size: 14px; margin: 0 8px 12px; transition: background-color 0.2s ease;\">" +
                "          Xem chi tiết" +
                "        </a>" +
                "      </div>" +

                "      <!-- Additional Info -->" +
                "      <div style=\"background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 24px 0;\">" +
                "        <h4 style=\"color: #212529; font-size: 14px; font-weight: 500; margin: 0 0 12px;\">💡 Khuyến nghị</h4>" +
                "        <p style=\"color: #6c757d; margin: 0; line-height: 1.6; font-size: 14px;\">" +
                "          Chúng tôi khuyến khích bạn dành thời gian đọc kỹ các thay đổi để hiểu rõ quyền lợi và nghĩa vụ của mình. " +
                "          Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với đội ngũ hỗ trợ của chúng tôi." +
                "        </p>" +
                "      </div>" +

                "      <!-- Support -->" +
                "      <div style=\"text-align: center; padding: 20px; background-color: #f8f9fa; border-radius: 6px; margin: 24px 0;\">" +
                "        <h4 style=\"margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin: 0 0 12px; color: #6c757d; font-size: 13px;\">Liên hệ với chúng tôi qua email</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color: #212529; text-decoration: none; font-weight: 500; font-size: 14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +

                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style=\"background-color: #f8f9fa; padding: 24px 32px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom: 16px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\" alt=\"Facebook\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\" alt=\"Instagram\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; opacity: 0.6; transition: opacity 0.2s ease;\">" +
                "          <img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\" alt=\"YouTube\" style=\"vertical-align: middle;\">" +
                "        </a>" +
                "      </div>" +
                "      <div style=\"font-size: 12px; color: #6c757d; margin-bottom: 8px;\">" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin: 0 8px; color: #6c757d; text-decoration: none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin: 0; font-size: 11px; color: #adb5bd;\">" +
                "        © 2025 SHOPPING. Tất cả quyền được bảo lưu." +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String fmtDateTime(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(DT_FMT) : "N/A";
    }

    private String safe(String s) {
        return s == null ? "" : StringEscapeUtils.escapeHtml4(s);
    }

    // ======= PUBLIC SENDERS =======
    public EmailReponse sendEmailSellerSuspension(SellerSuspensionEvent event) {
        String htmlContent = templateSellerSuspension(event);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder().name("SHOPPING").email(email).build())
                .to(List.of(Recipient.builder().email(event.getSellerEmail()).build()))
                .subject("Thông báo tạm khóa tài khoản bán hàng")
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send seller suspension email: " + e.contentUTF8());
        }
    }

    public EmailReponse sendEmailSellerWarning(SellerWarningEvent event) {
        String htmlContent = templateSellerWarning(event);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder().name("SHOPPING").email(email).build())
                .to(List.of(Recipient.builder().email(event.getSellerEmail()).build()))
                .subject("Cảnh báo vi phạm chính sách bán hàng")
                .htmlContent(htmlContent)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to send seller warning email: " + e.contentUTF8());
        }
    }

    // ======= TEMPLATES =======
    private String templateSellerSuspension(SellerSuspensionEvent e) {
        String daysStr = e.getSuspensionDays() != null ? e.getSuspensionDays() + " ngày" : "N/A";
        String endAt   = fmtDateTime(e.getSuspensionEndDate());

        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Tạm khóa tài khoản bán hàng</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; } body { margin:0; padding:0; }" +
                "    @media only screen and (max-width: 600px) { .container { width:100% !important; margin:10px !important; } .content{ padding:20px !important;} .header{ padding:30px 20px !important;} }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family:'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color:#f8f9fa; margin:0; padding:20px; line-height:1.6;\">" +
                "  <div class=\"container\" style=\"max-width:600px; margin:0 auto; background-color:#fff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,.05);\">" +

                "    <div class=\"header\" style=\"background-color:#fff; padding:40px 32px 30px; border-bottom:1px solid #f0f0f0; text-align:center;\">" +
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width:60px; height:auto; margin-bottom:20px;\"/>" +
                "      <h1 style=\"margin:0 0 8px; font-size:24px; font-weight:600; color:#212529; letter-spacing:-0.25px;\">Tài khoản bị tạm khóa</h1>" +
                "      <p style=\"margin:0; font-size:15px; color:#6c757d;\">Seller #" + safe(e.getSellerId()) + "</p>" +
                "    </div>" +

                "    <div class=\"content\" style=\"padding:32px;\">" +
                "      <div style=\"margin-bottom:24px;\">" +
                "        <h2 style=\"color:#212529; margin:0 0 8px; font-size:18px; font-weight:500;\">Kính gửi " + safe(e.getSellerEmail()) + ",</h2>" +
                "        <p style=\"color:#6c757d; font-size:15px; margin:0;\">Tài khoản bán hàng của bạn đã bị <strong>tạm khóa</strong> do vi phạm chính sách.</p>" +
                "      </div>" +

                "      <div style=\"background-color:#fef2f2; border-left:3px solid #ef4444; padding:16px 20px; border-radius:6px; margin:24px 0;\">" +
                "        <div style=\"display:flex; align-items:center; gap:8px; color:#b91c1c; font-weight:600;\">" +
                "          <span>❌</span><span>Thông tin tạm khóa</span>" +
                "        </div>" +
                "        <div style=\"margin-top:12px; color:#991b1b; font-size:14px;\">" +
                "          <div><strong>Loại vi phạm:</strong> " + safe(e.getViolationType()) + "</div>" +
                "          <div><strong>Số lần vi phạm:</strong> " + (e.getViolationCount() == null ? "N/A" : e.getViolationCount()) + "</div>" +
                "          <div><strong>Thời hạn:</strong> " + daysStr + "</div>" +
                "          <div><strong>Khóa đến:</strong> " + endAt + "</div>" +
                "          " + (e.getReason() != null && !e.getReason().isBlank()
                ? "<div style='margin-top:8px;'><strong>Lý do chi tiết:</strong> " + safe(e.getReason()) + "</div>"
                : "") +
                "        </div>" +
                "      </div>" +

                "      <div style=\"background-color:#fff7ed; border:1px solid #fed7aa; padding:20px; border-radius:6px; margin:24px 0;\">" +
                "        <h4 style=\"color:#ea580c; font-size:14px; font-weight:500; margin:0 0 8px; text-transform:uppercase; letter-spacing:.5px;\">Các bước tiếp theo</h4>" +
                "        <ul style=\"color:#9a3412; margin:0; padding-left:20px; line-height:1.6; font-size:14px;\">" +
                "          <li>Đọc lại chính sách và nội quy đăng bán</li>" +
                "          <li>Chuẩn bị tài liệu/chứng cứ nếu cần khiếu nại</li>" +
                "          <li>Đợi hết thời gian tạm khóa hoặc gửi yêu cầu khiếu nại</li>" +
                "        </ul>" +
                "      </div>" +

                "      <div style=\"text-align:center; margin:32px 0;\">" +
                "        <a href=\"http://localhost:3000/seller/violations\" style=\"display:inline-block; background-color:#212529; color:#fff; padding:12px 24px; text-decoration:none; border-radius:6px; font-weight:500; font-size:14px;\">Xem chi tiết vi phạm</a>" +
                "      </div>" +

                "      <div style=\"text-align:center; padding:20px; background-color:#f8f9fa; border-radius:6px; margin:24px 0;\">" +
                "        <h4 style=\"margin:0 0 8px; font-size:14px; font-weight:500; color:#212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin:0 0 12px; color:#6c757d; font-size:13px;\">Liên hệ email hỗ trợ</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color:#212529; text-decoration:none; font-weight:500; font-size:14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +

                "    <div style=\"background-color:#f8f9fa; padding:24px 32px; text-align:center; border-top:1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom:16px;\">" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\"/></a>" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\"/></a>" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\"/></a>" +
                "      </div>" +
                "      <div style=\"font-size:12px; color:#6c757d; margin-bottom:8px;\">" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin:0; font-size:11px; color:#adb5bd;\">© 2025 SHOPPING. Tất cả quyền được bảo lưu.</p>" +
                "    </div>" +
                "  </div>" +
                "</body></html>";
    }

    private String templateSellerWarning(SellerWarningEvent e) {
        return "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Cảnh báo vi phạm</title>" +
                "  <style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');" +
                "    * { box-sizing: border-box; } body{ margin:0; padding:0; }" +
                "    @media only screen and (max-width:600px){ .container{ width:100% !important; margin:10px !important;} .content{ padding:20px !important;} .header{ padding:30px 20px !important;} }" +
                "  </style>" +
                "</head>" +
                "<body style=\"font-family:'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color:#f8f9fa; margin:0; padding:20px; line-height:1.6;\">" +
                "  <div class=\"container\" style=\"max-width:600px; margin:0 auto; background:#fff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,.05);\">" +

                "    <div class=\"header\" style=\"background:#fff; padding:40px 32px 30px; border-bottom:1px solid #f0f0f0; text-align:center;\">" +
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png\" alt=\"SHOPPING\" style=\"width:60px; height:auto; margin-bottom:20px;\"/>" +
                "      <h1 style=\"margin:0 0 8px; font-size:24px; font-weight:600; color:#212529; letter-spacing:-0.25px;\">Cảnh báo vi phạm</h1>" +
                "      <p style=\"margin:0; font-size:15px; color:#6c757d;\">Seller #" + safe(e.getSellerId()) + "</p>" +
                "    </div>" +

                "    <div class=\"content\" style=\"padding:32px;\">" +
                "      <div style=\"margin-bottom:24px;\">" +
                "        <h2 style=\"color:#212529; margin:0 0 8px; font-size:18px; font-weight:500;\">Kính gửi " + safe(e.getSellerEmail()) + ",</h2>" +
                "        <p style=\"color:#6c757d; font-size:15px; margin:0;\">Chúng tôi ghi nhận vi phạm liên quan đến <strong>" + safe(e.getViolationType()) + "</strong>.</p>" +
                "      </div>" +

                "      <div style=\"background-color:#fffbeb; border-left:3px solid #f59e0b; padding:16px 20px; border-radius:6px; margin:24px 0;\">" +
                "        <div style=\"display:flex; align-items:center; gap:8px; color:#b45309; font-weight:600;\">" +
                "          <span>⚠️</span><span>Chi tiết cảnh báo</span>" +
                "        </div>" +
                "        <div style=\"margin-top:12px; color:#92400e; font-size:14px;\">" +
                "          <div><strong>Số lần vi phạm:</strong> " + (e.getViolationCount() == null ? "N/A" : e.getViolationCount()) + "</div>" +
                "          " + (e.getWarningMessage() != null && !e.getWarningMessage().isBlank()
                ? "<div style='margin-top:6px;'><strong>Thông điệp:</strong> " + safe(e.getWarningMessage()) + "</div>"
                : "") +
                "        </div>" +
                "      </div>" +

                "      <div style=\"background-color:#f8f9fa; border-radius:6px; padding:20px; margin:24px 0;\">" +
                "        <h4 style=\"color:#212529; font-size:14px; font-weight:500; margin:0 0 8px;\">Khuyến nghị xử lý</h4>" +
                "        <ul style=\"color:#6b7280; margin:0; padding-left:20px; line-height:1.6; font-size:14px;\">" +
                "          <li>Rà soát lại sản phẩm/tin đăng liên quan và chỉnh sửa theo chính sách</li>" +
                "          <li>Tránh tái diễn vi phạm để không bị tạm khóa tài khoản</li>" +
                "          <li>Giữ liên hệ với bộ phận hỗ trợ nếu cần hướng dẫn</li>" +
                "        </ul>" +
                "      </div>" +

                "      <div style=\"text-align:center; margin:32px 0;\">" +
                "        <a href=\"http://localhost:3000/seller/violations\" style=\"display:inline-block; background-color:#212529; color:#fff; padding:12px 24px; text-decoration:none; border-radius:6px; font-weight:500; font-size:14px;\">Xem chi tiết vi phạm</a>" +
                "      </div>" +

                "      <div style=\"text-align:center; padding:20px; background-color:#f8f9fa; border-radius:6px; margin:24px 0;\">" +
                "        <h4 style=\"margin:0 0 8px; font-size:14px; font-weight:500; color:#212529;\">Cần hỗ trợ?</h4>" +
                "        <p style=\"margin:0 0 12px; color:#6c757d; font-size:13px;\">Liên hệ email hỗ trợ</p>" +
                "        <a href=\"mailto:thinh183tt@gmail.com\" style=\"color:#212529; text-decoration:none; font-weight:500; font-size:14px;\">thinh183tt@gmail.com</a>" +
                "      </div>" +
                "    </div>" +

                "    <div style=\"background-color:#f8f9fa; padding:24px 32px; text-align:center; border-top:1px solid #e9ecef;\">" +
                "      <div style=\"margin-bottom:16px;\">" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/733/733547.png\" width=\"20\"/></a>" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111463.png\" width=\"20\"/></a>" +
                "        <a href=\"#\" style=\"margin:0 8px; opacity:.6;\"><img src=\"https://cdn-icons-png.flaticon.com/512/1384/1384060.png\" width=\"20\"/></a>" +
                "      </div>" +
                "      <div style=\"font-size:12px; color:#6c757d; margin-bottom:8px;\">" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Chính sách</a>" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Hỗ trợ</a>" +
                "        <a href=\"#\" style=\"margin:0 8px; color:#6c757d; text-decoration:none;\">Điều khoản</a>" +
                "      </div>" +
                "      <p style=\"margin:0; font-size:11px; color:#adb5bd;\">© 2025 SHOPPING. Tất cả quyền được bảo lưu.</p>" +
                "    </div>" +
                "  </div>" +
                "</body></html>";
    }

    /** Gửi email enforce tối giản: không tiết lộ nội dung chính sách. */
    public void sendEmailPolicyEnforcementMinimal(List<String> emails) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime deadline = start.plusDays(30);

        for (String recipient : emails) {
            String html = templatePolicyEnforcementMinimal(start, deadline);
            EmailRequest req = EmailRequest.builder()
                    .sender(Sender.builder().name("SHOPPING").email(email).build())
                    .to(List.of(Recipient.builder().email(recipient).build()))
                    .subject("Thông báo xác nhận chính sách trong 30 ngày")
                    .htmlContent(html)
                    .build();
            try {
                emailClient.sendEmail(apiKey, req);
            } catch (FeignException e) {
                throw new RuntimeException("Failed to send minimal policy enforcement email: " + e.contentUTF8());
            }
        }
    }

    /** Template tối giản: chỉ nói phải chấp nhận trong 30 ngày, không nêu chi tiết chính sách. */
    private String templatePolicyEnforcementMinimal(LocalDateTime startDate, LocalDateTime deadlineDate) {
        String effective = fmtDateTime(startDate);
        String deadline  = fmtDateTime(deadlineDate);

        return "<html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Thông báo xác nhận chính sách</title>"
                + "<style>@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap');*{box-sizing:border-box}body{margin:0;padding:0}</style>"
                + "</head><body style=\"font-family:Inter,system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;background:#f8f9fa;margin:0;padding:20px;line-height:1.6;\">"

                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.05)'>"
                + "  <div style='padding:36px 28px 22px;border-bottom:1px solid #f0f0f0;text-align:center'>"
                + "    <img src='https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png' alt='SHOPPING' style='width:56px;height:auto;margin-bottom:16px'/>"
                + "    <h1 style='margin:0 0 6px;font-size:20px;font-weight:700;color:#111827'>Thông báo xác nhận chính sách</h1>"
                + "    <p style='margin:0;color:#6b7280;font-size:13px'>Áp dụng từ " + effective + "</p>"
                + "  </div>"

                + "  <div style='padding:24px 28px'>"
                + "    <p style='margin:0 0 12px;color:#374151'>Kính gửi Quý Người bán,</p>"
                + "    <p style='margin:0 0 12px;color:#374151'>Để tiếp tục hoạt động bình thường, vui lòng <strong>chấp nhận</strong> các điều khoản cập nhật trên Bảng điều khiển Người bán.</p>"

                + "    <div style='background:#fff7ed;border-left:3px solid #f59e0b;padding:14px 16px;border-radius:8px;margin:16px 0'>"
                + "      <div style='color:#b45309;font-weight:700;font-size:14px;margin-bottom:6px'>Thời hạn xác nhận</div>"
                + "      <div style='color:#92400e;font-size:14px'>Trước: <strong>" + deadline + "</strong></div>"
                + "      <p style='margin:8px 0 0;color:#92400e;font-size:13px'>Sau thời hạn trên, nếu không có phản hồi, hợp tác sẽ <strong>tự động chấm dứt</strong>.</p>"
                + "    </div>"

                + "    <div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;padding:16px;margin:18px 0'>"
                + "      <div style='color:#111827;font-weight:700;margin-bottom:6px'>Lưu ý</div>"
                + "      <ul style='margin:0;padding-left:18px;color:#374151;font-size:14px;line-height:1.7'>"
                + "        <li>Một số quyền trong Seller Center có thể <strong>bị hạn chế tạm thời</strong> cho đến khi bạn chấp nhận.</li>"
                + "        <li>Chấp nhận xong, quyền sẽ được <strong>khôi phục đầy đủ</strong>.</li>"
                + "      </ul>"
                + "    </div>"

                + "    <div style='background:#fef2f2;border-left:3px solid #ef4444;padding:14px 16px;border-radius:8px;margin:16px 0'>"
                + "      <div style='color:#b91c1c;font-weight:700;margin-bottom:6px'>Muốn chấm dứt ngay?</div>"
                + "      <p style='margin:0 0 8px;color:#991b1b;font-size:14px'>Vui lòng <strong>xử lý các đơn hàng đang mở</strong> và <strong>đối soát/rút số dư ví</strong> trước khi yêu cầu chấm dứt.</p>"
                + "    </div>"

                + "    <div style='text-align:center;padding:14px;background:#f3f4f6;border-radius:8px;margin:18px 0'>"
                + "      <div style='font-weight:700;color:#111827;margin-bottom:6px'>Cần hỗ trợ?</div>"
                + "      <a href='mailto:thinh183tt@gmail.com' style='color:#111827;text-decoration:none;font-weight:700;font-size:14px'>thinh183tt@gmail.com</a>"
                + "    </div>"
                + "  </div>"

                + "  <div style='background:#f8f9fa;padding:20px 28px;text-align:center;border-top:1px solid #e5e7eb'>"
                + "    <p style='margin:0;color:#9ca3af;font-size:12px'>© 2025 SHOPPING. Tất cả quyền được bảo lưu.</p>"
                + "  </div>"
                + "</div>"

                + "</body></html>";
    }
    public EmailReponse sendEmailSellerUnsuspension(iuh.fit.event.dto.SellerUnsuspensionEvent e) {
        String html = templateSellerUnsuspension(e);
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder().name("SHOPPING").email(email).build())
                .to(List.of(Recipient.builder().email(e.getSellerEmail()).build()))
                .subject("Tài khoản bán hàng đã được khôi phục")
                .htmlContent(html)
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException ex) {
            throw new RuntimeException("Failed to send seller unsuspension email: " + ex.contentUTF8());
        }
    }

    private String templateSellerUnsuspension(iuh.fit.event.dto.SellerUnsuspensionEvent e) {
        String at = e.getUnsuspendedAt() != null
                ? e.getUnsuspendedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";

        return "<html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>"
                + "<title>Khôi phục tài khoản bán hàng</title>"
                + "<style>@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&display=swap');*{box-sizing:border-box}body{margin:0;padding:0}</style>"
                + "</head><body style=\"font-family:'Inter',system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;background:#f8f9fa;margin:0;padding:20px;line-height:1.6;\">"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.05)'>"
                + "  <div style='padding:40px 32px 30px;border-bottom:1px solid #f0f0f0;text-align:center'>"
                + "    <img src='https://res.cloudinary.com/dzidt15cl/image/upload/v1757179436/shopping_1_o7hhyi.png' alt='SHOPPING' style='width:60px;height:auto;margin-bottom:18px'/>"
                + "    <h1 style='margin:0 0 8px;font-size:24px;font-weight:600;color:#111827'>Tài khoản đã được khôi phục</h1>"
                + "    <p style='margin:0;color:#6b7280;font-size:14px'>Seller #" + (e.getSellerId()==null?"N/A":e.getSellerId()) + " • " + at + "</p>"
                + "  </div>"
                + "  <div style='padding:28px 32px'>"
                + "    <p style='margin:0 0 12px;color:#374151'>Kính gửi " + (e.getSellerEmail()==null?"quý người bán":e.getSellerEmail()) + ",</p>"
                + "    <p style='margin:0 0 12px;color:#374151'>Tài khoản bán hàng của bạn đã được <strong>khôi phục</strong>. Các tính năng và quyền đăng bán đã mở lại.</p>"
                + "    <div style='background:#ecfdf5;border:1px solid #a7f3d0;padding:16px;border-radius:8px;margin:16px 0'>"
                + "      <div style='color:#065f46;font-weight:600;margin-bottom:6px'>Gợi ý sau khi khôi phục</div>"
                + "      <ul style='margin:0;padding-left:18px;color:#065f46;font-size:14px;line-height:1.7'>"
                + "        <li>Rà soát lại danh sách sản phẩm và trạng thái tồn kho.</li>"
                + "        <li>Đọc kỹ Chính sách đăng bán để tránh tái vi phạm.</li>"
                + "        <li>Theo dõi mục Vi phạm trong Seller Center nếu còn cảnh báo mở.</li>"
                + "      </ul>"
                + "    </div>"
                + "    <div style='text-align:center;margin:24px 0'>"
                + "      <a href='http://localhost:3000/seller/dashboard' style='display:inline-block;background:#111827;color:#fff;padding:12px 20px;text-decoration:none;border-radius:8px;font-weight:600'>Vào Seller Center</a>"
                + "    </div>"
                + "    <div style='text-align:center;padding:16px;background:#f3f4f6;border-radius:8px;margin:18px 0'>"
                + "      <div style='font-weight:600;color:#111827;margin-bottom:6px'>Cần hỗ trợ?</div>"
                + "      <a href='mailto:thinh183tt@gmail.com' style='color:#111827;text-decoration:none;font-weight:600'>thinh183tt@gmail.com</a>"
                + "    </div>"
                + "  </div>"
                + "  <div style='background:#f8f9fa;padding:20px 28px;text-align:center;border-top:1px solid #e5e7eb'>"
                + "    <p style='margin:0;color:#9ca3af;font-size:12px'>© 2025 SHOPPING. Tất cả quyền được bảo lưu.</p>"
                + "  </div>"
                + "</div>"
                + "</body></html>";
    }

}