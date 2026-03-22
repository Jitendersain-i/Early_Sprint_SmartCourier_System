package com.smartcourier.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HUBS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hubs_seq")
    @SequenceGenerator(name = "hubs_seq", sequenceName = "HUBS_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
