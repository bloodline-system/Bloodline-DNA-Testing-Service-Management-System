package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.ResponseObject;
import com.dna_testing_system.dev.service.UploadImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/upload")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadImgController {

    UploadImageService uploadImageService;
    @PostMapping("/img")
    public ResponseEntity<ResponseObject> uploadImg(@RequestParam("file") MultipartFile file){
        try {
            // Save file to folder
            String generatedFileName = uploadImageService.saveImage(file);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "upload file successfully", generatedFileName)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                    new ResponseObject("ok", e.getMessage(), "")
            );
        }
    }

    // Get image url
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) {
        try {
            byte[] bytes = uploadImageService.readImageContent(fileName);
            String contentType = Files.probeContentType(Paths.get("uploads", fileName));
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("")
    public ResponseEntity<ResponseObject> getAllImage() {
            try {
                List<String> urls = uploadImageService.loadAll()
                        .map(path -> {
                            // Convert fileName to URL (send request "getImage")
                            String urlPath = MvcUriComponentsBuilder.fromMethodName(UploadImgController.class,
                                    "getImage", path.getFileName().toString()).build().toString();
                            return urlPath;
                        }).collect(Collectors.toList());
                return ResponseEntity.ok(new ResponseObject("ok", "List images successfully", urls));
            } catch (Exception e) {
                return ResponseEntity.ok(new ResponseObject("failed", "List images failed", new String[] {}));
            }
    }

}
