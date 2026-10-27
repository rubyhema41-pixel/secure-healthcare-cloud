package com.healthcare.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String username;
    public String action;
    public String detail;
    public Instant at = Instant.now();
}
