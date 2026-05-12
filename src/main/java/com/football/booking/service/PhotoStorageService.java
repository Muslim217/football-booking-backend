package com.football.booking.service;

import com.football.booking.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class PhotoStorageService {

    private final Path uploadDir;

    public PhotoStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Директория для загрузок: {}", this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для загрузок: " + uploadDir, e);
        }
    }

    /**
     * Сохраняет файл и возвращает относительный URL для хранения в БД.
     * Пример: /uploads/abc123.jpg
     */
    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Проверяем тип файла
        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new IllegalArgumentException("Допустимые форматы: jpg, jpeg, png, webp");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл сохранён: {}", targetPath);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + filename, e);
        }
    }

    /**
     * Удаляет файл по относительному URL (например /uploads/abc123.jpg)
     */
    public void delete(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            return;
        }
        String filename = relativeUrl.substring("/uploads/".length());
        Path filePath = uploadDir.resolve(filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл: {}", filePath);
        }
    }
}
