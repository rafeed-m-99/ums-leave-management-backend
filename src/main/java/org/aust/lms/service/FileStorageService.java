package org.aust.lms.service;

import org.aust.lms.dto.TempUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads/leave").toAbsolutePath().normalize();
    private final Path tempRoot = Paths.get("uploads/temp").toAbsolutePath().normalize();

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public FileStorageService() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(tempRoot);
    }

    // =========================
    // TEMP UPLOAD
    // =========================
    public TempUploadResponse saveTemp(MultipartFile file, String sessionId) {

        validate(file);

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;

        Path folder = tempRoot.resolve(sessionId);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path destination = folder.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new TempUploadResponse(
                storedName,
                originalName,
                file.getContentType(),
                file.getSize()
        );
    }

    // =========================
    // DELETE TEMP (CLEANUP ONLY)
    // =========================
    public void deleteTemp(String sessionId) {
        Path folder = tempRoot.resolve(sessionId);

        if (!Files.exists(folder)) return;

        try (var paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {}
    }

    // =========================
    // PERMANENT FILE ACCESS
    // =========================
    public Resource loadAsResource(String storedFileName, Long leaveId) {
        try {
            Path file = root.resolve(String.valueOf(leaveId))
                    .resolve(storedFileName)
                    .normalize();

            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String storedFileName, Long leaveId) {
        try {
            Path file = root.resolve(String.valueOf(leaveId))
                    .resolve(storedFileName)
                    .normalize();

            Files.deleteIfExists(file);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // VALIDATION
    // =========================
    private void validate(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File too large");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Invalid file type");
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf("."));
    }
}