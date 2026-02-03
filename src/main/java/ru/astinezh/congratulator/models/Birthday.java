package ru.astinezh.congratulator.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "BIRTHDAYS")
public class Birthday {
    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @OneToOne
    @JoinColumn(name = "celebrant_uuid")
    private UUID celebrant;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BirthdayStatus status;
}
