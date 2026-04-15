package com.dna_testing_system.dev.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileDownloadController {

    private static final String PRIMARY_UPLOAD_DIR = "uploads_information";
    private static final String FALLBACK_UPLOAD_DIR = "uploads";

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            Path filePath = resolveFilePath(filename);
            if (filePath == null) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam String filename) {
        try {
            Path filePath = resolveFilePath(filename);
            if (filePath == null) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Path resolveFilePath(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return null;
        }

        Path primaryDir = Paths.get(PRIMARY_UPLOAD_DIR).toAbsolutePath().normalize();
        Path filePath = primaryDir.resolve(filename).normalize();
        if (isValidFile(filePath, primaryDir)) {
            return filePath;
        }

        Path fallbackDir = Paths.get(FALLBACK_UPLOAD_DIR).toAbsolutePath().normalize();
        Path fallbackPath = fallbackDir.resolve(filename).normalize();
        if (isValidFile(fallbackPath, fallbackDir)) {
            return fallbackPath;
        }

        return null;
    }

    private boolean isValidFile(Path filePath, Path baseDirectory) {
        return filePath.startsWith(baseDirectory) && Files.exists(filePath) && !Files.isDirectory(filePath);
    }
}
