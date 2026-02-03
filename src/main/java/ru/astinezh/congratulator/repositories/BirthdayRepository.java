package ru.astinezh.congratulator.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ASTiNEZh.dto.BirthdayDto;

import java.util.UUID;

@Repository
public interface BirthdayRepository extends JpaRepository<BirthdayDto, UUID> {
}
