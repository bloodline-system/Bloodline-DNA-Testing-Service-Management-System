package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.entity.ContentPost;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.ContentPostRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.ContentPostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContentPostServiceImpl implements ContentPostService {

    ContentPostRepository contentPostRepository;
    UserRepository userRepository;

    @Override
    public List<ContentPostResponse> getAllPosts() {
        return contentPostRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ContentPostResponse getPostById(Long postId) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Not found post with id: " + postId));
        return toResponse(post);
    }

    @Override
    @Transactional
    public void savePost(ContentPostRequest request) {
        ContentPost post = new ContentPost();

        post.setPostTitle(request.getPostTitle());
        post.setPostContent(request.getPostContent());
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setPostCategory(request.getPostCategory());
        post.setTags(request.getTags());
        post.setPostStatus(request.getPostStatus());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User author = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        post.setViewCount(0L);
        post.setLikeCount(0);
        post.setShareCount(0);

        toResponse(contentPostRepository.save(post));
    }

    @Override
    @Transactional
    public void updatePost(Long postId, ContentPostRequest request) {
        //  Tim post theo ID
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Not found post with id: " + postId + " to update"));

        //  Update theo request
        post.setPostTitle(request.getPostTitle());
        post.setPostContent(request.getPostContent());
        post.setPostCategory(request.getPostCategory());
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setTags(request.getTags());
        post.setPostStatus(request.getPostStatus());
        post.setUpdatedAt(LocalDateTime.now());

        // Save to database
        contentPostRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        if (!contentPostRepository.existsById(postId)) {
            throw new RuntimeException("Not found post with id: " + postId + " to delete.");
        }
        contentPostRepository.deleteById(postId);
    }

    private ContentPostResponse toResponse(ContentPost post) {
        return ContentPostResponse.builder()
                .postId(post.getPostId())
                .postTitle(post.getPostTitle())
                .postContent(post.getPostContent())
                .postCategory(post.getPostCategory())
                .tags(post.getTags())
                .featuredImageUrl(post.getFeaturedImageUrl())
                .postStatus(post.getPostStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .shareCount(post.getShareCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .build();
    }
}
