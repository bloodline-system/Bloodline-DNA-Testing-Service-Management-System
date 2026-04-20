package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.manager.ManagerPostController;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.ContentPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ManagerPostController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ManagerPostBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ContentPostService contentPostService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== LIST (GET) - Paging + Filters BVA ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_pageMinus1_returns500_andDoesNotCallService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("page must be >= 0"));

        verify(contentPostService, never()).getPostsPage(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_page0_returns200_andCallsService() throws Exception {
        when(contentPostService.getPostsPage(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage(0, 10));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentPostService).getPostsPage(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_size0_returns500_andDoesNotCallService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("size must be > 0"));

        verify(contentPostService, never()).getPostsPage(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_defaultsToAllFilters_andSortCreatedAtDesc() throws Exception {
        when(contentPostService.getPostsPage(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage(0, 10));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PostStatus> statusCaptor = ArgumentCaptor.forClass(PostStatus.class);
        ArgumentCaptor<PostCategory> categoryCaptor = ArgumentCaptor.forClass(PostCategory.class);
        ArgumentCaptor<PostTag> tagCaptor = ArgumentCaptor.forClass(PostTag.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(contentPostService).getPostsPage(
                queryCaptor.capture(),
                statusCaptor.capture(),
                categoryCaptor.capture(),
                tagCaptor.capture(),
                pageableCaptor.capture()
        );

        assertEquals(null, queryCaptor.getValue());
        assertEquals(null, statusCaptor.getValue());
        assertEquals(null, categoryCaptor.getValue());
        assertEquals(null, tagCaptor.getValue());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        Sort.Order createdAtOrder = pageable.getSort().getOrderFor("createdAt");
        assertNotNull(createdAtOrder);
        assertEquals(Sort.Direction.DESC, createdAtOrder.getDirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_qProvided_passesThroughToService() throws Exception {
        when(contentPostService.getPostsPage(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage(0, 10));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(contentPostService).getPostsPage(queryCaptor.capture(), any(), any(), any(), any(Pageable.class));
        assertEquals("hello", queryCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_blankEnumParams_treatedAsAllNull() throws Exception {
        when(contentPostService.getPostsPage(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage(0, 10));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("status", "   ")
                        .param("category", "\t")
                        .param("tag", "\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PostStatus> statusCaptor = ArgumentCaptor.forClass(PostStatus.class);
        ArgumentCaptor<PostCategory> categoryCaptor = ArgumentCaptor.forClass(PostCategory.class);
        ArgumentCaptor<PostTag> tagCaptor = ArgumentCaptor.forClass(PostTag.class);
        verify(contentPostService).getPostsPage(any(), statusCaptor.capture(), categoryCaptor.capture(), tagCaptor.capture(), any(Pageable.class));
        assertEquals(null, statusCaptor.getValue());
        assertEquals(null, categoryCaptor.getValue());
        assertEquals(null, tagCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_parsesEnums_caseInsensitive_andAllAsNull() throws Exception {
        when(contentPostService.getPostsPage(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage(0, 10));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("status", "published")
                        .param("category", "testing_guide")
                        .param("tag", "dna_testing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PostStatus> statusCaptor = ArgumentCaptor.forClass(PostStatus.class);
        ArgumentCaptor<PostCategory> categoryCaptor = ArgumentCaptor.forClass(PostCategory.class);
        ArgumentCaptor<PostTag> tagCaptor = ArgumentCaptor.forClass(PostTag.class);

        verify(contentPostService).getPostsPage(any(), statusCaptor.capture(), categoryCaptor.capture(), tagCaptor.capture(), any(Pageable.class));

        assertEquals(PostStatus.PUBLISHED, statusCaptor.getValue());
        assertEquals(PostCategory.TESTING_GUIDE, categoryCaptor.getValue());
        assertEquals(PostTag.DNA_TESTING, tagCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_invalidEnum_returns500_andDoesNotCallService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("status", "BAD_VALUE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("No enum constant")));

        verify(contentPostService, never()).getPostsPage(any(), any(), any(), any(), any(Pageable.class));
    }

    // ==================== CREATE (POST) - Request Body BVA ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_postTitleBlank_returns400_andDoesNotCallService() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postTitle(" ")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_postTitleLength500_returns201() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postTitle("a".repeat(500))
                .build();

        when(contentPostService.createPost(any(ContentPostRequest.class))).thenReturn(sampleResponse(1L));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.postId").value(1));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_postTitleLength501_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postTitle("a".repeat(501))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_postContentBlank_returns400_andDoesNotCallService() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postContent(" ")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_featuredImageUrlLength1000_returns201() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .featuredImageUrl("h".repeat(1000))
                .build();

        when(contentPostService.createPost(any(ContentPostRequest.class))).thenReturn(sampleResponse(1L));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_featuredImageUrlLength1001_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .featuredImageUrl("h".repeat(1001))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_missingCategory_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postCategory(null)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).createPost(any());
    }

    // ==================== UPDATE (PUT) - Request Body BVA ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_postTitleLength501_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postTitle("a".repeat(501))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).updatePostReturning(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_postContentBlank_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postContent(" ")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).updatePostReturning(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_missingCategory_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .postCategory(null)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).updatePostReturning(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_featuredImageUrlLength1001_returns400() throws Exception {
        ContentPostRequest request = validRequestBuilder()
                .featuredImageUrl("h".repeat(1001))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).updatePostReturning(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_valid_returns200() throws Exception {
        ContentPostRequest request = validRequestBuilder().build();
        when(contentPostService.updatePostReturning(eq(5L), any(ContentPostRequest.class)))
                .thenReturn(sampleResponse(5L));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.postId").value(5));

        verify(contentPostService).updatePostReturning(eq(5L), any(ContentPostRequest.class));
    }

    // ==================== UPDATE STATUS (PATCH) - Enum BVA ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_missingParam_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_blankParam_returns500() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5)
                        .param("status", "   "))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Missing required parameter"));

        verify(contentPostService, never()).updatePostStatus(anyLong(), any());
    }

        @Test
        @WithMockUser(roles = "MANAGER")
        void updateStatus_emptyString_returns500() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5)
                                                .param("status", ""))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("Missing required parameter"));

                verify(contentPostService, never()).updatePostStatus(anyLong(), any());
        }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_invalidEnum_returns500_andDoesNotCallService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5)
                        .param("status", "BAD_VALUE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("No enum constant")));

        verify(contentPostService, never()).updatePostStatus(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_valid_caseInsensitive_returns200() throws Exception {
        when(contentPostService.updatePostStatus(eq(5L), eq(PostStatus.PUBLISHED)))
                .thenReturn(sampleResponse(5L));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5)
                        .param("status", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.postId").value(5));

        verify(contentPostService).updatePostStatus(5L, PostStatus.PUBLISHED);
    }

    // ==================== DELETE (DELETE) - hard vs soft ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_defaultsToSoftDelete_returns200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/manager/posts/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentPostService).softDeletePost(10L);
        verify(contentPostService, never()).deletePost(anyLong());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_hardTrue_usesHardDelete_returns200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/manager/posts/{id}", 10)
                        .param("hard", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentPostService).deletePost(10L);
        verify(contentPostService, never()).softDeletePost(anyLong());
    }

        @Test
        @WithMockUser(roles = "MANAGER")
        void delete_invalidHardParam_returns500_andDoesNotCallService() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/manager/posts/{id}", 10)
                                                .param("hard", "not-a-bool"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.code").value(500));

                verify(contentPostService, never()).softDeletePost(anyLong());
                verify(contentPostService, never()).deletePost(anyLong());
        }

    // ==================== GET BY ID (GET) ====================

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_returns200_whenFound() throws Exception {
        when(contentPostService.getPostById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.postId").value(1));

        verify(contentPostService).getPostById(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_returns500_whenServiceThrows() throws Exception {
        when(contentPostService.getPostById(1L)).thenThrow(new NoSuchElementException("Not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts/{id}", 1))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Not found"));
    }

        @Test
        @WithMockUser(roles = "MANAGER")
        void getById_idZero_returns200_whenServiceReturns() throws Exception {
                when(contentPostService.getPostById(0L)).thenReturn(sampleResponse(0L));

                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts/{id}", 0))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.postId").value(0));

                verify(contentPostService).getPostById(0L);
        }

    private static PageResponse<ContentPostResponse> emptyPage(int pageNumber, int pageSize) {
        return PageResponse.<ContentPostResponse>builder()
                .content(List.of())
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }

    private static ContentPostResponse sampleResponse(Long id) {
        return ContentPostResponse.builder()
                .postId(id)
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(PostStatus.DRAFT)
                .authorId("u1")
                .build();
    }

    private static ContentPostRequest.ContentPostRequestBuilder validRequestBuilder() {
        return ContentPostRequest.builder()
                .postTitle("Valid title")
                .postContent("Valid content")
                .featuredImageUrl("https://example.com/img.jpg")
                .postCategory(PostCategory.TESTING_GUIDE)
                .tags(java.util.Set.of(PostTag.DNA_TESTING, PostTag.TIPS))
                .postStatus(PostStatus.DRAFT);
    }
}
