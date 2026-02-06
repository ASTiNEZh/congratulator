package ru.astinezh.congratulator.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.astinezh.congratulator.models.Celebrant;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final Path storagePath;
    private final CelebrantService celebrantService;

    public FileService(@Value("${app.storage.root}") String storageDir, CelebrantService celebrantService) {
        this.storagePath = Paths.get(storageDir).toAbsolutePath();
        this.celebrantService = celebrantService;

        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create storage folder: " + storagePath, e);
        }
    }

    public List<String> saveFiles(List<MultipartFile> files, UUID uuid) throws IOException {
        List<String> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            UUID id = UUID.randomUUID();
            String filename = file.getOriginalFilename();
            String storedName = id + "_" + filename;

            Path filePath = storagePath.resolve(storedName);

            Celebrant celebrant = celebrantService.findOne(uuid);
            celebrant.setPhotoStoragePath(filePath.toString());
            celebrantService.update(uuid, celebrant);

            Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW);

            savedFiles.add(storedName);
        }

        return savedFiles;
    }

    public ByteArrayResource getFileAsResource(UUID uuid) throws IOException {
        Celebrant celebrant = celebrantService.findOne(uuid);
        String storedName = celebrant.getPhotoStoragePath();

        Path filePath = storagePath.resolve(storedName);

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + storedName);
        }

        return new ByteArrayResource(Files.readAllBytes(filePath));
    }
}
