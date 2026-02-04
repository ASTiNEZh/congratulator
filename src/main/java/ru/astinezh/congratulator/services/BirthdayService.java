package ru.astinezh.congratulator.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.repositories.BirthdayRepository;

import java.util.List;
import java.util.UUID;

@Service
public class BirthdayService {
    private final BirthdayRepository birthdayRepository;

    @Autowired
    public BirthdayService(BirthdayRepository birthdayRepository) {
        this.birthdayRepository = birthdayRepository;
    }

    public List<Birthday> findAll() {
        return birthdayRepository.findAll();
    }

    public Birthday findOne(UUID uuid) {
        return birthdayRepository.findById(uuid).orElse(null);
    }

    public void remember(Birthday birthday) {
        birthdayRepository.save(birthday);
    }

    public void update(UUID uuid, Birthday birthday) {
        birthday.setUuid(uuid);
        birthdayRepository.save(birthday);
    }

    public void delete(UUID uuid) {
        birthdayRepository.deleteById(uuid);
    }
}
