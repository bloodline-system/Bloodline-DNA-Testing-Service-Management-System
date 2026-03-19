package com.dna_testing_system.dev.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface UploadImageService {
    String saveImage(MultipartFile file);
    Stream<Path> loadAll(); // load all file inside a folder
    byte[] readImageContent(String fileName);
    void deleteAllImages();
}
