package iuh.fit.se.config;

import iuh.fit.event.dto.NotificationEvent;
import iuh.fit.event.dto.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public DefaultKafkaConsumerFactory<String, OrderCreatedEvent> orderEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notify-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // tin cậy packages của bạn
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "iuh.fit.event,iuh.fit.event.dto");
        // quan trọng: bỏ dùng type headers của producer
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        JsonDeserializer<OrderCreatedEvent> value = new JsonDeserializer<>(OrderCreatedEvent.class);
        value.addTrustedPackages("iuh.fit.event", "iuh.fit.event.dto");
        value.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(value)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
    orderEventKafkaListenerContainerFactory(DefaultKafkaConsumerFactory<String, OrderCreatedEvent> cf,
                                            KafkaTemplate<String, Object> template) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>();
        factory.setConsumerFactory(cf);

        // DLT: gửi record lỗi sang {topic}.DLT và không retry
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
        handler.addNotRetryableExceptions(SerializationException.class, DeserializationException.class);
        factory.setCommonErrorHandler(handler);

        return factory;
    }

    // Tương tự cho NotificationEvent
    @Bean
    public DefaultKafkaConsumerFactory<String, NotificationEvent> notificationEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notify-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "iuh.fit.event,iuh.fit.event.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        JsonDeserializer<NotificationEvent> value = new JsonDeserializer<>(NotificationEvent.class);
        value.addTrustedPackages("iuh.fit.event", "iuh.fit.event.dto");
        value.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(value)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
    notificationEventKafkaListenerContainerFactory(
            DefaultKafkaConsumerFactory<String, NotificationEvent> cf,
            KafkaTemplate<String, Object> template) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>();
        factory.setConsumerFactory(cf);

        var recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
        handler.addNotRetryableExceptions(SerializationException.class, DeserializationException.class);
        factory.setCommonErrorHandler(handler);

        return factory;
    }
}
