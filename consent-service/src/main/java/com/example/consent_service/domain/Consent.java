package com.example.consent_service.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "consents")
@Data
public class Consent {
    @Id
    private String receiverNumber;
    private Boolean optIn;
    private Instant updatedAt;
}
