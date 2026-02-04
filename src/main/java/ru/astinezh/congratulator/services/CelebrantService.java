package ru.astinezh.congratulator.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.repositories.CelebrantRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CelebrantService {
    private final CelebrantRepository celebrantRepository;

    @Autowired
    public CelebrantService(CelebrantRepository celebrantRepository) {
        this.celebrantRepository = celebrantRepository;
    }

    public List<Celebrant> findAll() {
        return celebrantRepository.findAll();
    }

    public Celebrant findOne(UUID uuid) {
        return celebrantRepository.findById(uuid).orElse(null);
    }

    public void remember(Celebrant celebrant) {
        celebrantRepository.save(celebrant);
    }

    public void update(UUID uuid, Celebrant celebrant) {
        celebrant.setUuid(uuid);
        celebrantRepository.save(celebrant);
    }

    public void delete(UUID uuid) {
        celebrantRepository.deleteById(uuid);
    }
}
