package ru.astinezh.congratulator.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.astinezh.congratulator.models.Birthday;


import java.util.UUID;

@Repository
public interface BirthdayRepository extends JpaRepository<Birthday, UUID> {
    Page<Birthday> findAll(Pageable pageable);
}
