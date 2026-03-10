package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.service.UploadImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadImageServiceImpl implements UploadImageService {
    Path uploadImgdir = Paths.get("uploads");

    public UploadImageServiceImpl() {
        try {
            Files.createDirectories(uploadImgdir);
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize storage folder!", e);
        }
    }
    private boolean isImage(MultipartFile file) {
        // Let install filenameutils to check file extension
        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        return Arrays.asList(new String[] {"png", "jpg", "jpeg", "bmp"})
                .contains(fileExtension.trim().toLowerCase());
    }
    @Override
    public String saveImage(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to upload empty file!");
            }
            // Check file is image?
            if (!isImage(file)) {
                throw new RuntimeException("File is not an image!");
            }
            // File must be less than 10MB
            float fileSizeInMB = file.getSize() / 1_000_000.0f;
            if (fileSizeInMB > 10.0f) {
                throw new RuntimeException("File size must be less than 10MB!");
            }
            // File must be rename
            String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
            String generatedFileName = UUID.randomUUID().toString().replace("-", "") + "." + fileExtension;
            Path destinationFilePath = this.uploadImgdir.resolve(
                    Paths.get(generatedFileName))
                    .normalize().toAbsolutePath();
            if (!destinationFilePath.getParent().equals(this.uploadImgdir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory!");
            }
            try (InputStream inputStrteam = file.getInputStream()){
                    Files.copy(inputStrteam, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return generatedFileName;
        }
        catch (IOException e) {
                throw new RuntimeException("Failed to upload file!", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.uploadImgdir, 1)
                    .filter(path -> !path.equals(this.uploadImgdir))
                    .map(this.uploadImgdir::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored files!", e);
        }
    }

    @Override
    public byte[] readImageContent(String fileName) {
        try {
            Path file = uploadImgdir.resolve(fileName);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                return bytes;
            }
            else {
                throw new RuntimeException("Cannot read file content!" + fileName + " does not exist!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file content!", e);
        }
    }

    @Override
    public void deleteAllImages() {

    }
}
