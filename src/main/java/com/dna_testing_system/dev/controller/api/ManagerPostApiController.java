package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.service.ContentPostService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/posts")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerPostApiController {

    ContentPostService contentPostService;

    @GetMapping
    public ResponseEntity<PageResponse<ContentPostResponse>> list(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "status", required = false, defaultValue = "all") String status,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category,
            @RequestParam(value = "tag", required = false, defaultValue = "all") String tag,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        PostStatus parsedStatus = parseEnum(status, PostStatus.class);
        PostCategory parsedCategory = parseEnum(category, PostCategory.class);
        PostTag parsedTag = parseEnum(tag, PostTag.class);

        PageResponse<ContentPostResponse> postsPage = contentPostService.getPostsPage(
                query,
                parsedStatus,
                parsedCategory,
                parsedTag,
                pageable
        );

        return ResponseEntity.ok(postsPage);
    }

    @PostMapping
    public ResponseEntity<ContentPostResponse> create(@Valid @RequestBody ContentPostRequest request) {
        ContentPostResponse created = contentPostService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentPostResponse> getById(@PathVariable("id") Long id) {
        ContentPostResponse post = contentPostService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentPostResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ContentPostRequest request
    ) {
        ContentPostResponse updated = contentPostService.updatePostReturning(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ContentPostResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status
    ) {
        PostStatus parsedStatus = parseRequiredEnum(status, PostStatus.class);
        ContentPostResponse updated = contentPostService.updatePostStatus(id, parsedStatus);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable("id") Long id,
            @RequestParam(value = "hard", required = false, defaultValue = "false") boolean hard
    ) {
        if (hard) {
            contentPostService.deletePost(id);
        } else {
            contentPostService.softDeletePost(id);
        }

        return ResponseEntity.ok(Map.of("message", "Post deleted successfully"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bad request"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleServerError(Exception ex) {
        log.error("Unhandled error in ManagerPostApiController", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
    }

    private static <T extends Enum<T>> T parseEnum(String raw, Class<T> enumClass) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        return Enum.valueOf(enumClass, raw.trim().toUpperCase());
    }

    private static <T extends Enum<T>> T parseRequiredEnum(String raw, Class<T> enumClass) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter");
        }
        return Enum.valueOf(enumClass, raw.trim().toUpperCase());
    }
}
