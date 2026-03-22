package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.service.FileEdit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileEditImpl implements FileEdit {
    @Override
    public String editFile(MultipartFile file) {
        String uploadsDir = "uploads_information/";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadsDir + fileName);
            try {
                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String finalFile = "/uploads_information/" + fileName;
        return finalFile;
    }

    @Override
    public void deleteFile(String fileName) {
        // Chuyển về đường dẫn vật lý
        String fileSystemPath = fileName.replaceFirst("/", ""); // "uploads/abcxyz.jpg"
        Path oldImagePath = Paths.get(fileSystemPath);
        try {
            Files.deleteIfExists(oldImagePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
