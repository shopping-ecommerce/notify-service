package iuh.fit.se.service.impl;

import feign.FeignException;
import iuh.fit.event.dto.OrderCreatedEvent;
import iuh.fit.event.dto.OrderItemPayload;
import iuh.fit.event.dto.OrderStatusChangedEvent;
import iuh.fit.event.dto.SellerVerificationEvent;
import iuh.fit.se.dto.request.Recipient;
import iuh.fit.se.dto.response.EmailReponse;
import iuh.fit.se.dto.request.EmailRequest;
import iuh.fit.se.dto.request.SendEmailRequest;
import iuh.fit.se.dto.request.Sender;
import iuh.fit.se.repository.httpclient.EmailClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private String templateSellerOrderCancellation(OrderStatusChangedEvent order) {
        String statusText = "Đã hủy";
        String statusColor = "#ef4444"; // Red for CANCELLED
        String statusIcon = "❌";
        String statusMessage = "Đơn hàng của bạn đã bị khách hàng hủy. Vui lòng xem chi tiết bên dưới.";

        // Build cancellation reason section
        String cancellationReasonHtml = "";
        if (order.getReason() != null && !order.getReason().trim().isEmpty()) {
            cancellationReasonHtml =
                    "      <div style=\"background-color: #fef2f2; border-left: 3px solid #ef4444; padding: 16px 20px; border-radius: 6px; margin: 24px 0;\">" +
                            "        <h4 style=\"color: #dc2626; font-size: 14px; font-weight: 500; margin: 0 0 8px; text-transform: uppercase; letter-spacing: 0.5px;\">Lý do hủy đơn</h4>" +
                            "        <p style=\"color: #991b1b; margin: 0; line-height: 1.5; font-size: 14px;\">" + order.getReason() + "</p>" +
                            "      </div>";
        }

        // Build order items table
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
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
                                "        <div style=\"font-size: 13px; color: #6c757d;\">Size: %s</div>" +
                                "      </div>" +
                                "    </div>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                                "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                                "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%,.0f₫</span>" +
                                "  </td>" +
                                "</tr>",
                        item.getProductName(),
                        item.getSize() != null ? item.getSize() : "N/A",
                        item.getQuantity(),
                        item.getSubTotal()
                ));
            }
        }

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
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #22c55e; font-weight: 500; font-size: 14px;\">Miễn phí</span>" +
                "        </div>" +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + String.format("%,.0f₫", order.getSubtotal()) + "</span>" +
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
                itemsHtml.append(String.format(
                        "<tr>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; vertical-align: top;\">" +
                                "    <div style=\"display: flex; align-items: center;\">" +
                                "      <div style=\"width: 40px; height: 40px; background-color: #f8f9fa; border-radius: 6px; margin-right: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #e9ecef;\">" +
                                "        <span style=\"color: #6c757d; font-size: 16px;\">📦</span>" +
                                "      </div>" +
                                "      <div>" +
                                "        <div style=\"font-weight: 500; color: #212529; font-size: 15px; margin-bottom: 2px;\">%s</div>" +
                                "        <div style=\"font-size: 13px; color: #6c757d;\">Size: %s</div>" +
                                "      </div>" +
                                "    </div>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                                "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                                "  </td>" +
                                "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                                "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%,.0f₫</span>" +
                                "  </td>" +
                                "</tr>",
                        item.getProductName(),
                        item.getSize() != null ? item.getSize() : "N/A",
                        item.getQuantity(),
                        item.getSubTotal()
                ));
            }
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
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #22c55e; font-weight: 500; font-size: 14px;\">Miễn phí</span>" +
                "        </div>" +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + String.format("%,.0f₫", order.getSubtotal()) + "</span>" +
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
                "http://localhost:8888/savorgo/api/authentication/verifyFromEmail?email=%s",
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
                "    <p style=\"text-align: center; margin-top: 30px; font-size: 14px;\">Nếu bạn không yêu cầu mã này, vui lòng liên hệ <a href=\"mailto:thinh183tt@gmail.com\">support@savorgo.com</a>.</p>" +

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
                "    <p style=\"text-align: center; margin-top: 30px; font-size: 14px;\">Nếu bạn gặp bất kỳ vấn đề nào, vui lòng liên hệ <a href=\"mailto:thinh183tt@gmail.com\">support@savorgo.com</a>.</p>" +
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
                            "        <div style=\"font-size: 13px; color: #6c757d;\">Size: %s</div>" +
                            "      </div>" +
                            "    </div>" +
                            "  </td>" +
                            "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: center; vertical-align: top;\">" +
                            "    <span style=\"background-color: #f8f9fa; padding: 6px 12px; border-radius: 12px; font-weight: 500; color: #495057; font-size: 14px;\">%d</span>" +
                            "  </td>" +
                            "  <td style=\"padding: 16px; border-bottom: 1px solid #f0f0f0; text-align: right; vertical-align: top;\">" +
                            "    <span style=\"font-weight: 600; color: #212529; font-size: 15px;\">%,.0f₫</span>" +
                            "  </td>" +
                            "</tr>",
                    item.getProductName(), item.getSize(), item.getQuantity(), item.getSubTotal()
            ));
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
                "          <span style=\"color: #6c757d; font-size: 14px;\">Phí vận chuyển:</span>" +
                "          <span style=\"color: #22c55e; font-weight: 500; font-size: 14px;\">Miễn phí</span>" +
                "        </div>" +
                "        <hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 16px 0;\">" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "          <span style=\"font-size: 16px; font-weight: 500; color: #212529;\">Tổng cộng:</span>" +
                "          <span style=\"font-size: 20px; font-weight: 600; color: #212529;\">" + String.format("%,.0f₫", order.getSubtotal()) + "</span>" +
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

}