package ru.astinezh.congratulator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.ASTiNEZh.controller.BirthdaysApi;
import ru.ASTiNEZh.dto.BirthdayDto;

import java.util.List;
import java.util.UUID;

@RestController
public class BirthdayController implements BirthdaysApi {
    @Override
    public ResponseEntity<Void> rememberBirthday(BirthdayDto birthdayDto) {
        return BirthdaysApi.super.rememberBirthday(birthdayDto);
    }

    @Override
    public ResponseEntity<Void> deleteBirthday(UUID uuid) {
        return BirthdaysApi.super.deleteBirthday(uuid);
    }

    @Override
    public ResponseEntity<BirthdayDto> getBirthday(UUID uuid) {
        return BirthdaysApi.super.getBirthday(uuid);
    }

    @Override
    public ResponseEntity<List<BirthdayDto>> getBirthdaysList(Integer page, Integer size, String sort) {
        return BirthdaysApi.super.getBirthdaysList(page, size, sort);
    }

    @Override
    public ResponseEntity<Void> updateBirthday(UUID uuid, BirthdayDto birthdayDto) {
        return BirthdaysApi.super.updateBirthday(uuid, birthdayDto);
    }
}
