package com.dna_testing_system.dev.controller;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileDownloadController {

    private final String UPLOAD_DIR = "uploads_information";

    private Path resolveUploadDirPath() {
        // Prefer a folder that exists, to avoid confusion about the app working directory.
        Path primary = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        if (Files.exists(primary) && Files.isDirectory(primary)) {
            return primary;
        }

        // Common case when running the app from the workspace root.
        Path secondary = Paths.get("bloodline-dna-testing_BE", UPLOAD_DIR).toAbsolutePath().normalize();
        if (Files.exists(secondary) && Files.isDirectory(secondary)) {
            return secondary;
        }

        // Fallback to primary even if it doesn't exist (so errors become consistent 404).
        return primary;
    }

    @GetMapping("/files/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            // Đảm bảo chỉ lấy tên file, không cho phép truyền ../ để bảo mật
            if (isInvalidFilename(filename)) {
                return ResponseEntity.badRequest().build();
            }
            Path uploadDirPath = resolveUploadDirPath();
            Path filePath = uploadDirPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDirPath) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/view")
    public ResponseEntity<Resource> viewFile(@RequestParam String filename) {
        try {
            // Bảo mật: chỉ cho phép tên file, không chứa ký tự đặc biệt
            if (isInvalidFilename(filename)) {
                return ResponseEntity.badRequest().build();
            }
            Path uploadDirPath = resolveUploadDirPath();
            Path filePath = uploadDirPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDirPath) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Content-Disposition: inline để xem trực tiếp (ví dụ PDF sẽ hiển thị trên trình duyệt)
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/v1/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename,
                                            @RequestParam(value = "disposition", defaultValue = "inline") String disposition) {
        try {
            if (isInvalidFilename(filename)) {
                return ResponseEntity.badRequest().build();
            }

            Path uploadDirPath = resolveUploadDirPath();
            Path filePath = uploadDirPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDirPath) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String normalizedDisposition = disposition == null ? "inline" : disposition.trim().toLowerCase();
            String contentDisposition = normalizedDisposition.equals("attachment") ? "attachment" : "inline";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + "; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isInvalidFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return true;
        }
        return filename.contains("..") || filename.contains("/") || filename.contains("\\");
    }
}
