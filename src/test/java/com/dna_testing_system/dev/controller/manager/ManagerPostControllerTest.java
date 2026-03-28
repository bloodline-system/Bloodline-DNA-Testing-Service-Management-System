package com.dna_testing_system.dev.controller.manager;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class ManagerPostControllerTest {

    @Autowired
    MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    ContentPostService contentPostService;

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void listDefaultsToAllFiltersAndCreatedAtDesc() throws Exception {
        PageResponse<ContentPostResponse> page = PageResponse.<ContentPostResponse>builder()
                .content(List.of(ContentPostResponse.builder().postId(1L).postTitle("t").build()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(contentPostService.getPostsPage(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get posts successfully"))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.content[0].postId").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(contentPostService).getPostsPage(isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void listParsesEnumsCaseInsensitiveAndAllAsNull() throws Exception {
        PageResponse<ContentPostResponse> page = PageResponse.<ContentPostResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();

        when(contentPostService.getPostsPage(eq("hello"), eq(PostStatus.DRAFT), eq(PostCategory.NEWS), eq(PostTag.HEALTH), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("q", "hello")
                        .param("status", "draft")
                        .param("category", "news")
                        .param("tag", "health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentPostService).getPostsPage(eq("hello"), eq(PostStatus.DRAFT), eq(PostCategory.NEWS), eq(PostTag.HEALTH), any(Pageable.class));

        reset(contentPostService);
        when(contentPostService.getPostsPage(eq("hello"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("q", "hello")
                        .param("status", "all")
                        .param("category", "all")
                        .param("tag", "all"))
                .andExpect(status().isOk());

        verify(contentPostService).getPostsPage(eq("hello"), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void listReturns500WhenPageNegative() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("page", "-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("page must be >= 0"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void listReturns500WhenSizeNonPositive() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("size", "0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("size must be > 0"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void listReturns500WhenEnumIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts")
                        .param("status", "bad_value"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("No enum constant")));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReturns201WhenValid() throws Exception {
        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.NEWS)
                .tags(Set.of(PostTag.HEALTH))
                .postStatus(PostStatus.DRAFT)
                .build();

        ContentPostResponse created = ContentPostResponse.builder()
                .postId(123L)
                .postTitle("Title")
                .build();

        when(contentPostService.createPost(any(ContentPostRequest.class))).thenReturn(created);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Create post successfully"))
                .andExpect(jsonPath("$.data.postId").value(123));

        verify(contentPostService).createPost(any(ContentPostRequest.class));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReturns400WhenValidationFails() throws Exception {
        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle(" ")
                .postContent(" ")
                .postCategory(null)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.postTitle", containsString("must not be blank")))
                .andExpect(jsonPath("$.data.postContent", containsString("must not be blank")))
                .andExpect(jsonPath("$.data.postCategory", containsString("must not be null")));

        verify(contentPostService, never()).createPost(any());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getByIdReturnsOk() throws Exception {
        when(contentPostService.getPostById(1L)).thenReturn(ContentPostResponse.builder().postId(1L).postTitle("t").build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get post successfully"))
                .andExpect(jsonPath("$.data.postId").value(1));

        verify(contentPostService).getPostById(1L);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getByIdReturns500WhenServiceThrows() throws Exception {
        when(contentPostService.getPostById(1L)).thenThrow(new NoSuchElementException("Not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/manager/posts/{id}", 1L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateReturnsOkWhenValid() throws Exception {
        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.NEWS)
                .build();

        when(contentPostService.updatePostReturning(eq(9L), any(ContentPostRequest.class)))
                .thenReturn(ContentPostResponse.builder().postId(9L).postTitle("Title").build());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update post successfully"))
                .andExpect(jsonPath("$.data.postId").value(9));

        verify(contentPostService).updatePostReturning(eq(9L), any(ContentPostRequest.class));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateReturns400WhenValidationFails() throws Exception {
        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle(" ")
                .postContent(" ")
                .postCategory(null)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/manager/posts/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(contentPostService, never()).updatePostReturning(anyLong(), any());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateStatusReturnsOkWhenValidAndCaseInsensitive() throws Exception {
        when(contentPostService.updatePostStatus(5L, PostStatus.PUBLISHED))
                .thenReturn(ContentPostResponse.builder().postId(5L).postStatus(PostStatus.PUBLISHED).build());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5L)
                        .param("status", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update post status successfully"))
                .andExpect(jsonPath("$.data.postId").value(5));

        verify(contentPostService).updatePostStatus(5L, PostStatus.PUBLISHED);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateStatusReturns500WhenStatusBlank() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5L)
                        .param("status", " "))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Missing required parameter"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateStatusReturns400WhenStatusMissing() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/manager/posts/{id}/status", 5L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void deleteDefaultsToSoftDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/manager/posts/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Post deleted successfully"))
                .andExpect(jsonPath("$.data.message").value("Post deleted successfully"));

        verify(contentPostService).softDeletePost(7L);
        verify(contentPostService, never()).deletePost(anyLong());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void deleteUsesHardDeleteWhenHardTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/manager/posts/{id}", 7L)
                        .param("hard", "true"))
                .andExpect(status().isOk());

        verify(contentPostService).deletePost(7L);
        verify(contentPostService, never()).softDeletePost(anyLong());
    }
}
