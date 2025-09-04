package com.example.consent_service.application;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentController {
    private final ConsentService service;

    @PostMapping("/inbound-sms")
    public ResponseEntity<ConsentResponse> inbound(@RequestBody ConsentRequest request) {
        Boolean optIn = service.handleInboundSms(request.receiver, request.text);
        return ResponseEntity.ok(ConsentResponse.builder()
                .receiverNumber(request.receiver)
                .optIn(optIn)
                .build());
    }

    @GetMapping("/{receiver}")
    public ResponseEntity<ConsentResponse> getConsent(@PathVariable String receiver) {
       Boolean optIn = service.getConsent(receiver);
        return ResponseEntity.ok(ConsentResponse.builder()
                .receiverNumber(receiver)
                .optIn(optIn)
                .build());
    }
}
