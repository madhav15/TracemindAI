package com.tracemindai.preprocessor.kafka;

import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.event.PrintRequestEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {
    private final KafkaProperties kafkaProperties;

    private Map<String, Object> getProducerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG,
            kafkaProperties.getProducer().getAcks());
        configProps.put(ProducerConfig.RETRIES_CONFIG,
            kafkaProperties.getProducer().getRetries());
        return configProps;
    }

    @Bean
    public ProducerFactory<String, EmailRequestEvent> emailProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getProducerConfig());
    }

    @Bean
    public ProducerFactory<String, PrintRequestEvent> printProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getProducerConfig());
    }

    @Bean
    public KafkaTemplate<String, EmailRequestEvent> emailKafkaTemplate() {
        return new KafkaTemplate<>(emailProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, PrintRequestEvent> printKafkaTemplate() {
        return new KafkaTemplate<>(printProducerFactory());
    }
}
