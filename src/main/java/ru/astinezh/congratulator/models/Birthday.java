package ru.astinezh.congratulator.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

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
    private Celebrant сelebrant;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private BirthdayStatus status;
}
