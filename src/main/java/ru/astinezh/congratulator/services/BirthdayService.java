package ru.astinezh.congratulator.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.repositories.BirthdayRepository;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BirthdayService {
    private final BirthdayRepository birthdayRepository;

    @Autowired
    public BirthdayService(BirthdayRepository birthdayRepository) {
        this.birthdayRepository = birthdayRepository;
    }

    public List<Birthday> findAll(int page, int size, String sortBy) {
        Sort sort = sortBy.equals("desc")
                ? Sort.by(Sort.Direction.DESC, "birthDate")
                : Sort.by(Sort.Direction.ASC, "birthDate");

        return birthdayRepository.findAll(PageRequest.of(page, size, sort)).getContent();
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

    public List<Birthday> findActual() {
        List<Birthday> birthdays = birthdayRepository.findAll();
        List<Birthday> actualBirthdays = new ArrayList<>();

        LocalDate nowDate = LocalDate.now();
        LocalDate lastDate = nowDate.plusDays(30);

        for (Birthday birthday : birthdays) {

            MonthDay monthDay = MonthDay.from(birthday.getBirthDate());
            LocalDate birthdayThisYear = monthDay.atYear(nowDate.getYear());

            if (birthdayThisYear.isBefore(nowDate)) {
                birthdayThisYear = birthdayThisYear.plusYears(1);
            }
            if(!birthdayThisYear.isAfter(lastDate))
                actualBirthdays.add(birthday);
        }

        return actualBirthdays;
    }
}
