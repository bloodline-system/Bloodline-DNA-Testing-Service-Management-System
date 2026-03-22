package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.service.ContentPostService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/posts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiManagerPostController {

    ContentPostService contentPostService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContentPostResponse>>> getPosts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "status", required = false, defaultValue = "all") String status,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category,
            @RequestParam(value = "tag", required = false, defaultValue = "all") String tag,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            HttpServletRequest request
    ) {
        try {
            PostStatus parsedStatus = parseEnum(status, PostStatus.class);
            PostCategory parsedCategory = parseEnum(category, PostCategory.class);
            PostTag parsedTag = parseEnum(tag, PostTag.class);

            PageResponse<ContentPostResponse> page = contentPostService.getPostsPage(
                    query,
                    parsedStatus,
                    parsedCategory,
                    parsedTag,
                    pageable
            );

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Posts loaded", page));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Error loading posts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load posts", request.getRequestURI())
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentPostResponse>> getPostById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        try {
            ContentPostResponse post = contentPostService.getPostById(id);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Post loaded", post));
        } catch (Exception e) {
            log.error("Error loading post id={}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Post not found", request.getRequestURI())
            );
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContentPostResponse>> createPost(
            @Valid @RequestBody ContentPostRequest postRequest,
            HttpServletRequest request
    ) {
        try {
            ContentPostResponse created = contentPostService.createPost(postRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(HttpStatus.CREATED.value(), "Post created", created));
        } catch (Exception e) {
            log.error("Error creating post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to create post", request.getRequestURI())
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentPostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody ContentPostRequest postRequest,
            HttpServletRequest request
    ) {
        try {
            ContentPostResponse updated = contentPostService.updatePostReturning(id, postRequest);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Post updated", updated));
        } catch (Exception e) {
            log.error("Error updating post id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to update post", request.getRequestURI())
            );
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContentPostResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            HttpServletRequest request
    ) {
        try {
            PostStatus parsedStatus = parseRequiredEnum(status, PostStatus.class);
            ContentPostResponse updated = contentPostService.updatePostStatus(id, parsedStatus);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Post status updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Error updating post status id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to update post status", request.getRequestURI())
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id,
            @RequestParam(value = "hard", required = false, defaultValue = "false") boolean hard,
            HttpServletRequest request
    ) {
        try {
            if (hard) {
                contentPostService.deletePost(id);
            } else {
                contentPostService.softDeletePost(id);
            }
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Post deleted", null));
        } catch (Exception e) {
            log.error("Error deleting post id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to delete post", request.getRequestURI())
            );
        }
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
