package iuh.fit.se;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
public class NotificationServiceSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceSpringBootApplication.class, args);
    }

}
