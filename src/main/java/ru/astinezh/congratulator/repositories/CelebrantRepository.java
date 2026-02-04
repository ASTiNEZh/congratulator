package ru.astinezh.congratulator.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astinezh.congratulator.models.Celebrant;

import java.util.UUID;

public interface CelebrantRepository extends JpaRepository<Celebrant, UUID> {
}
