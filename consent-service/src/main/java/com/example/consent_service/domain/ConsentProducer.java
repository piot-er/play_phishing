package com.example.consent_service.domain;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsentProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ConsentProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishConsent(String receiver, Boolean status) {
        String json = String.format("{\"receiver\":\"%s\",\"optIn\":\"%s\"}", receiver, status);
        kafkaTemplate.send("consent.state", receiver, json);
        System.out.println(">>> Consent published: " + json);
    }
}
