package iuh.fit.se.controller;

import iuh.fit.se.dto.response.EmailReponse;
import iuh.fit.se.dto.request.SendEmailRequest;
import iuh.fit.se.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class EmailController {
    EmailService emailService;

    @PostMapping("/email/send")
    ResponseEntity<EmailReponse> sendEmail(@RequestBody SendEmailRequest emailRequest){

        return ResponseEntity.ok(emailService.sendEmail(emailRequest));
    }


}