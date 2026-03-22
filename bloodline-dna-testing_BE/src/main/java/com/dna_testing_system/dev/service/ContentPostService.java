package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;

import java.util.List;

public interface ContentPostService {
    List<ContentPostResponse> getAllPosts();
    ContentPostResponse getPostById(Long postId);
    void savePost(ContentPostRequest request);
    void updatePost(Long postId, ContentPostRequest request);
    void deletePost(Long postId);
}
