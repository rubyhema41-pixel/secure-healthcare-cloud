package com.healthcare.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class EhrRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long ownerId;
    public String fileName;
    @JsonIgnore public String s3Key;
    /** access policy: attributes a user must ALL have, e.g. "doctor,cardiology" */
    public String policy;
    /** SHA-256 of the encrypted file, used to detect tampering */
    public String sha256;
    @JsonIgnore @Column(length = 512) public String wrappedKey;
    @JsonIgnore @Column(length = 2000) public String keywordTokens;
    public Instant uploadedAt = Instant.now();
}
