package com.dna_testing_system.dev.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileEdit {
    String editFile(MultipartFile filePath);
    void deleteFile(String filePath);
}
