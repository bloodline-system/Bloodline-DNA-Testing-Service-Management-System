package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.exception.ApplicationException;
import com.dna_testing_system.dev.service.ContentPostService;
import com.dna_testing_system.dev.service.UploadImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContentPostController {
    ContentPostService contentPostService;
    UploadImageService uploadImageService;

    // Hien thi danh sach bai viet dang co
    @GetMapping(value = "")
    public String showPostList(Model model) {
        model.addAttribute("posts", contentPostService.getAllPosts());
        return "manager/posts";
    }

    // Hien thi form tao bai viet moi
    // localhost:8080/manager/posts/create
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("pageTitle", "CREATE NEW POST | Bloodline DNA TSMS");
        model.addAttribute("post", new ContentPostRequest());
        return "/manager/form";
    }

    // Save a new post
    // localhost:8080/manager/posts/save
    @PostMapping("/save")
    public String savePost(@ModelAttribute("post") ContentPostRequest request,
                           @RequestParam("imageFile") MultipartFile imageFile,
                           RedirectAttributes redirectAttributes) {
        if (!imageFile.isEmpty()) {
            String savedFileName = uploadImageService.saveImage(imageFile);
            // Gan url
            String imageUrl = "/upload/files/" + savedFileName;
            request.setFeaturedImageUrl(imageUrl);
        }
        contentPostService.savePost(request);
        redirectAttributes.addFlashAttribute("message", "New post saved successfully!");
        return "redirect:/manager/posts";
    }

    // Hien thi form sua bai viet
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        ContentPostResponse response = contentPostService.getPostById(id);
        if (response == null) {
            return "redirect:/manager/posts";
        }

        ContentPostRequest request = new ContentPostRequest();

        request.setPostId(id);
        request.setPostTitle(response.getPostTitle());
        request.setPostContent(response.getPostContent());
        request.setFeaturedImageUrl(response.getFeaturedImageUrl());
        request.setPostCategory(response.getPostCategory());
        request.setTags(response.getTags());
        request.setPostStatus(response.getPostStatus());
        // Gan post vao model
        model.addAttribute("post", request);
        model.addAttribute("postId", id);
        model.addAttribute("pageTitle", "EDIT POST | " + response.getPostTitle());
        return "/manager/form";
    }

    // Xu ly cap nhat bai viet
    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable("id") Long id,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             @ModelAttribute("post") ContentPostRequest request,
                             RedirectAttributes redirectAttributes ) {
        if (!imageFile.isEmpty()) {
            String savedFileName = uploadImageService.saveImage(imageFile);
            // Gan url
            String imageUrl = "/upload/files/" + savedFileName;
            request.setFeaturedImageUrl(imageUrl);
        }
        contentPostService.updatePost(id, request);
        redirectAttributes.addFlashAttribute("message", "Post updated successfully!");
        return "redirect:/manager/posts";
    }

    // Xoa bai viet
    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {

        try {
            contentPostService.deletePost(id);
            redirectAttributes.addFlashAttribute("message", "Post deleted successfully!");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/manager/posts";
    }
}
