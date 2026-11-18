package ru.astinezh.congratulator.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import ru.ASTiNEZh.controller.BirthdaysApi;
import ru.ASTiNEZh.controller.DefaultApi;
import ru.ASTiNEZh.dto.BirthdayDto;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.services.BirthdayService;
import ru.astinezh.congratulator.services.CelebrantService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class BirthdayController implements BirthdaysApi, DefaultApi {
    private final BirthdayService birthdayService;
    private final CelebrantService celebrantService;
    private final ModelMapper modelMapper;

    @Autowired
    public BirthdayController(
            BirthdayService birthdayService,
            CelebrantService celebrantService,
            ModelMapper modelMapper
    ) {
        this.birthdayService = birthdayService;
        this.celebrantService = celebrantService;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<Void> rememberBirthday(BirthdayDto birthday) {
        celebrantService.remember(modelMapper.map(birthday.getCelebrant(), Celebrant.class));
        birthdayService.remember(modelMapper.map(birthday, Birthday.class));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return BirthdaysApi.super.getRequest();
    }

    @Override
    public ResponseEntity<Void> deleteBirthday(UUID uuid) {
        birthdayService.delete(uuid);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<BirthdayDto> getBirthday(UUID uuid) {
        return ResponseEntity.ok(modelMapper.map(birthdayService.findOne(uuid), BirthdayDto.class));
    }

    @Override
    public ResponseEntity<List<BirthdayDto>> getBirthdaysList(Integer page, Integer size, String sort) {
        return ResponseEntity.ok(
                birthdayService.findAll(page, size, sort).stream().map(this::toDto).collect(Collectors.toList())
        );
    }

    @Override
    public ResponseEntity<Void> updateBirthday(UUID uuid, BirthdayDto birthdayDto) {
        Celebrant celebrant = modelMapper.map(birthdayDto.getCelebrant(), Celebrant.class);
        celebrantService.update(celebrant.getUuid(), celebrant);
        birthdayService.update(uuid, modelMapper.map(birthdayDto, Birthday.class));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private BirthdayDto toDto(Birthday birthday) {
        return modelMapper.map(birthday, BirthdayDto.class);
    }

    @Override
    public ResponseEntity<List<BirthdayDto>> getActualBirthdaysList() {
        return ResponseEntity.ok(
                birthdayService.findActual().stream().map(this::toDto).collect(Collectors.toList())
        );
    }
}
