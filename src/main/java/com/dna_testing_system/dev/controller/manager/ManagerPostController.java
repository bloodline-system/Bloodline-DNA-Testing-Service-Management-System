package com.dna_testing_system.dev.controller.manager;

import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.service.ContentPostService;
import com.dna_testing_system.dev.service.UploadImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager/posts")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerPostController {

    ContentPostService contentPostService;
    UploadImageService uploadImageService;

    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "status", required = false, defaultValue = "all") String status,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category,
            @RequestParam(value = "tag", required = false, defaultValue = "all") String tag,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model
    ) {
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

        model.addAttribute("postsPage", postsPage);
        model.addAttribute("posts", postsPage.getContent());

        model.addAttribute("q", query);
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        model.addAttribute("tag", tag);

        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("categories", PostCategory.values());
        model.addAttribute("tags", PostTag.values());
        model.addAttribute("pageTitle", "Post Management");

        return "manager/posts";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("post", new ContentPostRequest());
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("categories", PostCategory.values());
        model.addAttribute("tags", PostTag.values());
        model.addAttribute("pageTitle", "Create New Post");
        return "manager/blog-form";
    }

    @PostMapping
    public String create(
            @ModelAttribute("post") ContentPostRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String savedFileName = uploadImageService.saveImage(imageFile);
                request.setFeaturedImageUrl("/upload/files/" + savedFileName);
            }

            contentPostService.createPost(request);
            redirectAttributes.addFlashAttribute("message", "Post created successfully!");
        } catch (Exception e) {
            log.error("Failed to create post", e);
            redirectAttributes.addFlashAttribute("message", "Failed to create post: " + e.getMessage());
        }

        return "redirect:/manager/posts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ContentPostResponse response = contentPostService.getPostById(id);

            ContentPostRequest request = new ContentPostRequest();
            request.setPostId(response.getPostId());
            request.setPostTitle(response.getPostTitle());
            request.setPostContent(response.getPostContent());
            request.setFeaturedImageUrl(response.getFeaturedImageUrl());
            request.setPostCategory(response.getPostCategory());
            request.setTags(response.getTags());
            request.setPostStatus(response.getPostStatus());

            model.addAttribute("post", request);
            model.addAttribute("postId", id);
            model.addAttribute("statuses", PostStatus.values());
            model.addAttribute("categories", PostCategory.values());
            model.addAttribute("tags", PostTag.values());
            model.addAttribute("pageTitle", "Edit Post");

            return "manager/blog-form";
        } catch (Exception e) {
            log.error("Failed to load edit form for post id={}", id, e);
            redirectAttributes.addFlashAttribute("message", "Post not found");
            return "redirect:/manager/posts";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable("id") Long id,
            @ModelAttribute("post") ContentPostRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String savedFileName = uploadImageService.saveImage(imageFile);
                request.setFeaturedImageUrl("/upload/files/" + savedFileName);
            }

            contentPostService.updatePostReturning(id, request);
            redirectAttributes.addFlashAttribute("message", "Post updated successfully!");
        } catch (Exception e) {
            log.error("Failed to update post id={}", id, e);
            redirectAttributes.addFlashAttribute("message", "Failed to update post: " + e.getMessage());
        }
        return "redirect:/manager/posts";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PostStatus parsed = parseRequiredEnum(status, PostStatus.class);
            contentPostService.updatePostStatus(id, parsed);
            redirectAttributes.addFlashAttribute("message", "Status updated successfully!");
        } catch (Exception e) {
            log.error("Failed to update status for post id={}", id, e);
            redirectAttributes.addFlashAttribute("message", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/manager/posts";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            @RequestParam(value = "hard", required = false, defaultValue = "false") boolean hard,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (hard) {
                contentPostService.deletePost(id);
            } else {
                contentPostService.softDeletePost(id);
            }
            redirectAttributes.addFlashAttribute("message", "Post deleted successfully!");
        } catch (Exception e) {
            log.error("Failed to delete post id={}", id, e);
            redirectAttributes.addFlashAttribute("message", "Failed to delete post: " + e.getMessage());
        }
        return "redirect:/manager/posts";
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
