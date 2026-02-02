package ru.astinezh.congratulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class CongratulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CongratulatorApplication.class, args);

//        BirthdayController bc = new BirthdayController();
//        UUID uuid = UUID.randomUUID();
//        bc.rememberBirthday(new BirthdayDto(uuid, BirthdayDto.StatusEnum.OTHER));
//        bc.deleteBirthday(uuid);
    }

}
