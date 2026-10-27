package com.healthcare.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class AccessRequest {
    public enum Status { PENDING, APPROVED, REJECTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long recordId;
    public Long userId;
    public String username;
    public String fileName;
    @Enumerated(EnumType.STRING) public Status status = Status.PENDING;
    public Instant createdAt = Instant.now();
}
