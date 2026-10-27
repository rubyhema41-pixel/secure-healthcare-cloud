package com.healthcare.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String username;
    @JsonIgnore public String passwordHash;
    @Enumerated(EnumType.STRING) public Role role;
    /** comma separated attributes, e.g. "doctor,cardiology,hospitalA" */
    public String attributes;
}
