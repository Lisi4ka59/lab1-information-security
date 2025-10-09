package com.lisi4ka.lab1informationsecurity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data @RequiredArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
}
