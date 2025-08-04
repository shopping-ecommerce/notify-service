package iuh.fit.se.service;

import feign.FeignException;
import io.github.cdimascio.dotenv.Dotenv;
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
public class EmailService {
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
                            .name("SavorGO")
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
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1747687548/ChatGPT_Image_May_20_2025_03_44_52_AM_lbs3mh.png\" alt=\"Logo\" style=\"max-width: 100px; height: auto; border-radius: 10px;\"/>" +
                "    </div>" +

                // Nội dung chính
                "    <h2 style=\"color: #333; text-align: center;\">Xác thực tài khoản SavorGO</h2>" +
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
                "      © 2025 SavorGO. Mọi quyền được bảo lưu." +
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
                "      <img src=\"https://res.cloudinary.com/dzidt15cl/image/upload/v1747687548/ChatGPT_Image_May_20_2025_03_44_52_AM_lbs3mh.png\" alt=\"Logo\" style=\"max-width: 100px; height: auto; border-radius: 10px;\"/>" +
                "    </div>" +
                // Nội dung chính
                "    <h2 style=\"color: #333; text-align: center;\">Chúc mừng bạn đã đăng ký thành công!</h2>" +
                "    <p>Xin chào <strong>" + (email != null ? email : "Khách hàng") + "</strong>,</p>" +
                "    <p style=\"text-align: center;\">Tài khoản của bạn với email <strong>" + name + "</strong> đã được tạo thành công.</p>" +
                "    <p style=\"text-align: center;\">Bây giờ bạn có thể đăng nhập và bắt đầu trải nghiệm các dịch vụ tuyệt vời của SavorGO!</p>" +
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
                "      © 2025 SavorGO. Mọi quyền được bảo lưu." +
                "    </p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}