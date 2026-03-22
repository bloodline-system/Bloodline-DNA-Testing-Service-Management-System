package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContentPostService {
    List<ContentPostResponse> getAllPosts();
    ContentPostResponse getPostById(Long postId);
    void savePost(ContentPostRequest request);
    void updatePost(Long postId, ContentPostRequest request);
    void deletePost(Long postId);

    PageResponse<ContentPostResponse> getPostsPage(String query,
                                                  PostStatus status,
                                                  PostCategory category,
                                                  PostTag tag,
                                                  Pageable pageable);

    ContentPostResponse createPost(ContentPostRequest request);
    ContentPostResponse updatePostReturning(Long postId, ContentPostRequest request);
    ContentPostResponse updatePostStatus(Long postId, PostStatus status);
    void softDeletePost(Long postId);
}
