package ru.astinezh.congratulator.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import ru.ASTiNEZh.controller.FilesApi;
import ru.astinezh.congratulator.services.FileService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class FilesController implements FilesApi {
    private final FileService fileService;

    public FilesController(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public ResponseEntity<Resource> filesIdGet(UUID id) {
        ResponseEntity<Resource> response = null;
        try {
            response = ResponseEntity.ok(fileService.getFileAsResource(id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    @Override
    public ResponseEntity<Void> filesUploadPost(List<MultipartFile> files, UUID uuid) {
        List<String> savedFiles = null;
        try {
            savedFiles = fileService.saveFiles(files, uuid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
