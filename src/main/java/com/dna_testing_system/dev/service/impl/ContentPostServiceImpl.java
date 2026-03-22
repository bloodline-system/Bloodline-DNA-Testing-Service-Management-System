package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.entity.ContentPost;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.repository.ContentPostRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.specification.ContentPostSpecifications;
import com.dna_testing_system.dev.service.ContentPostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContentPostServiceImpl implements ContentPostService {

    ContentPostRepository contentPostRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ContentPostResponse> getAllPosts() {
        return contentPostRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContentPostResponse getPostById(Long postId) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Not found post with id: " + postId));
        return toResponse(post);
    }

    @Override
    @Transactional
    public void savePost(ContentPostRequest request) {
        createPost(request);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, ContentPostRequest request) {
        updatePostReturning(postId, request);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        if (!contentPostRepository.existsById(postId)) {
            throw new RuntimeException("Not found post with id: " + postId + " to delete.");
        }
        contentPostRepository.deleteById(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContentPostResponse> getPostsPage(String query,
                                                         PostStatus status,
                                                         PostCategory category,
                                                         PostTag tag,
                                                         Pageable pageable) {

        Page<ContentPost> page = contentPostRepository.findAll(
                Specification
                        .where(ContentPostSpecifications.search(query))
                        .and(ContentPostSpecifications.hasStatus(status))
                        .and(ContentPostSpecifications.hasCategory(category))
                        .and(ContentPostSpecifications.hasTag(tag)),
                pageable
        );

        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public ContentPostResponse createPost(ContentPostRequest request) {
        ContentPost post = new ContentPost();

        post.setPostTitle(request.getPostTitle());
        post.setPostContent(request.getPostContent());
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setPostCategory(request.getPostCategory());
        post.setTags(normalizeTags(request.getTags()));

        PostStatus status = request.getPostStatus() != null ? request.getPostStatus() : PostStatus.DRAFT;
        post.setPostStatus(status);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        post.setAuthor(author);

        post.setCreatedAt(LocalDateTime.now());
        post.setViewCount(0L);
        post.setLikeCount(0);
        post.setShareCount(0);

        if (status == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(contentPostRepository.save(post));
    }

    @Override
    @Transactional
    public ContentPostResponse updatePostReturning(Long postId, ContentPostRequest request) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Not found post with id: " + postId + " to update"));

        post.setPostTitle(request.getPostTitle());
        post.setPostContent(request.getPostContent());
        post.setPostCategory(request.getPostCategory());
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setTags(normalizeTags(request.getTags()));

        PostStatus currentStatus = post.getPostStatus() != null ? post.getPostStatus() : PostStatus.DRAFT;
        PostStatus status = request.getPostStatus() != null ? request.getPostStatus() : currentStatus;
        post.setPostStatus(status);

        if (post.getViewCount() == null) {
            post.setViewCount(0L);
        }
        if (post.getLikeCount() == null) {
            post.setLikeCount(0);
        }
        if (post.getShareCount() == null) {
            post.setShareCount(0);
        }

        post.setUpdatedAt(LocalDateTime.now());

        if (status == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(contentPostRepository.save(post));
    }

    @Override
    @Transactional
    public ContentPostResponse updatePostStatus(Long postId, PostStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Post status must not be null");
        }

        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Not found post with id: " + postId + " to update status"));

        post.setPostStatus(status);
        post.setUpdatedAt(LocalDateTime.now());

        if (status == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(contentPostRepository.save(post));
    }

    @Override
    @Transactional
    public void softDeletePost(Long postId) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Not found post with id: " + postId + " to delete"));

        post.setPostStatus(PostStatus.DELETED);
        post.setUpdatedAt(LocalDateTime.now());
        contentPostRepository.save(post);
    }

    private ContentPostResponse toResponse(ContentPost post) {
        Set<PostTag> safeTags = post.getTags() == null ? new HashSet<>() : new HashSet<>(post.getTags());
        return ContentPostResponse.builder()
                .postId(post.getPostId())
                .postTitle(post.getPostTitle())
                .postContent(post.getPostContent())
                .postCategory(post.getPostCategory())
                .tags(safeTags)
                .featuredImageUrl(post.getFeaturedImageUrl())
                .postStatus(post.getPostStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .shareCount(post.getShareCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .authorId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .build();
    }

    private static Set<PostTag> normalizeTags(Set<PostTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tags);
    }
}
