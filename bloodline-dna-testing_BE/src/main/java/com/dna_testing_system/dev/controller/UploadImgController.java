package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.response.ImageInfoResponse;
import com.dna_testing_system.dev.dto.response.ImageUploadResponse;
import com.dna_testing_system.dev.dto.response.ResponseObject;
import com.dna_testing_system.dev.service.UploadImageService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadImgController {

    UploadImageService uploadImageService;
    @PostMapping("/upload/img")
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
    @GetMapping("/upload/files/{fileName:.+}")
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

    @GetMapping("/upload")
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

    @PostMapping("/api/v1/images")
    @ResponseBody
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImageV1(@RequestParam("file") MultipartFile file,
                                                                          HttpServletRequest request) {
        try {
            String generatedFileName = uploadImageService.saveImage(file);
            String url = MvcUriComponentsBuilder.fromMethodName(UploadImgController.class,
                "getImageV1", generatedFileName).build().toString();

            ImageUploadResponse response = new ImageUploadResponse(generatedFileName, url);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Upload image successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI()));
        }
    }

    @GetMapping("/api/v1/images")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ImageInfoResponse>>> listImagesV1(HttpServletRequest request) {
        try {
            List<ImageInfoResponse> images = uploadImageService.loadAll()
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        String url = MvcUriComponentsBuilder.fromMethodName(UploadImgController.class,
                                "getImageV1", fileName).build().toString();
                        return new ImageInfoResponse(fileName, url);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "List images successfully", images));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "List images failed", request.getRequestURI()));
        }
    }

    @GetMapping("/api/v1/images/{fileName:.+}")
    public ResponseEntity<byte[]> getImageV1(@PathVariable String fileName) {
        try {
            if (isInvalidFilename(fileName)) {
                return ResponseEntity.badRequest().build();
            }

            byte[] bytes = uploadImageService.readImageContent(fileName);
            String contentType = Files.probeContentType(Paths.get("uploads", fileName));
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isInvalidFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return true;
        }
        return filename.contains("..") || filename.contains("/") || filename.contains("\\");
    }

}
