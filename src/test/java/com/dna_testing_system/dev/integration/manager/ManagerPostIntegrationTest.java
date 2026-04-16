package com.dna_testing_system.dev.integration.manager;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.entity.ContentPost;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.ContentPostRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ManagerPostIntegrationTest extends AbstractIntegrationTest {

    @Autowired
        WebApplicationContext webApplicationContext;

        MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ContentPostRepository contentPostRepository;

    @Autowired
    UserRepository userRepository;

    /**
     * Prevent Redis stream listener from starting during @SpringBootTest.
     * The real bean comes from RedisStreamConfig.mailStreamListenerContainer(...), which connects to Redis.
     */
    @MockitoBean
    StreamMessageListenerContainer<String, ?> mailStreamListenerContainer;

    @BeforeEach
    void seedManagerUser() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        contentPostRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createPost_persistsDefaultsAndAuthor() throws Exception {
        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Integration Title")
                .postContent("Integration Content")
                .postCategory(PostCategory.NEWS)
                .tags(Set.of(PostTag.HEALTH))
                .postStatus(null)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/manager/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Create post successfully"))
                .andExpect(jsonPath("$.data.postId").isNumber())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long postId = root.path("data").path("postId").asLong();

        ContentPost saved = contentPostRepository.findById(postId).orElseThrow();
        assertThat(saved.getPostTitle()).isEqualTo("Integration Title");
        assertThat(saved.getPostContent()).isEqualTo("Integration Content");
        assertThat(saved.getPostCategory()).isEqualTo(PostCategory.NEWS);
        assertThat(saved.getPostStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getViewCount()).isEqualTo(0L);
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getShareCount()).isEqualTo(0);
        assertThat(saved.getTags()).containsExactlyInAnyOrder(PostTag.HEALTH);
        assertThat(saved.getAuthor()).isNotNull();
        assertThat(saved.getAuthor().getUsername()).isEqualTo("manager");
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getById_returnsPost() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();
        ContentPost post = ContentPost.builder()
                .author(author)
                .postTitle("T1")
                .postContent("C1")
                .postCategory(PostCategory.NEWS)
                .tags(Set.of(PostTag.NEWS))
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        post = contentPostRepository.save(post);

        mockMvc.perform(get("/api/v1/manager/posts/{id}", post.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get post successfully"))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.postTitle").value("T1"))
                .andExpect(jsonPath("$.data.authorId").value(author.getId()));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void list_filtersByStatus_publishedOnly() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();

        ContentPost draft = ContentPost.builder()
                .author(author)
                .postTitle("Draft")
                .postContent("Draft Content")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        ContentPost published = ContentPost.builder()
                .author(author)
                .postTitle("Published")
                .postContent("Published Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(PostStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        contentPostRepository.save(draft);
        contentPostRepository.save(published);

        mockMvc.perform(get("/api/v1/manager/posts")
                        .param("status", "published")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get posts successfully"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].postTitle").value("Published"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void update_updatesPersistedFields() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();
        ContentPost existing = ContentPost.builder()
                .author(author)
                .postTitle("Old")
                .postContent("Old Content")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        existing = contentPostRepository.save(existing);

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("New")
                .postContent("New Content")
                .postCategory(PostCategory.ANNOUNCEMENTS)
                .featuredImageUrl("http://example.com/img.png")
                .tags(Set.of(PostTag.TIPS, PostTag.HEALTH))
                .postStatus(null)
                .build();

        mockMvc.perform(put("/api/v1/manager/posts/{id}", existing.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update post successfully"))
                .andExpect(jsonPath("$.data.postId").value(existing.getPostId()))
                .andExpect(jsonPath("$.data.postTitle").value("New"));

        ContentPost updated = contentPostRepository.findById(existing.getPostId()).orElseThrow();
        assertThat(updated.getPostTitle()).isEqualTo("New");
        assertThat(updated.getPostContent()).isEqualTo("New Content");
        assertThat(updated.getPostCategory()).isEqualTo(PostCategory.ANNOUNCEMENTS);
        assertThat(updated.getFeaturedImageUrl()).isEqualTo("http://example.com/img.png");
        assertThat(updated.getTags()).containsExactlyInAnyOrder(PostTag.TIPS, PostTag.HEALTH);
        assertThat(updated.getUpdatedAt()).isNotNull();
        // postStatus remains DRAFT if request.postStatus is null
        assertThat(updated.getPostStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateStatus_setsPublishedAndPublishedAt() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();
        ContentPost existing = ContentPost.builder()
                .author(author)
                .postTitle("T")
                .postContent("C")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        existing = contentPostRepository.save(existing);

        mockMvc.perform(patch("/api/v1/manager/posts/{id}/status", existing.getPostId())
                        .param("status", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update post status successfully"))
                .andExpect(jsonPath("$.data.postStatus").value("PUBLISHED"));

        ContentPost updated = contentPostRepository.findById(existing.getPostId()).orElseThrow();
        assertThat(updated.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void delete_softDelete_setsStatusDeleted() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();
        ContentPost existing = ContentPost.builder()
                .author(author)
                .postTitle("T")
                .postContent("C")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        existing = contentPostRepository.save(existing);

        mockMvc.perform(delete("/api/v1/manager/posts/{id}", existing.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Post deleted successfully"));

        ContentPost updated = contentPostRepository.findById(existing.getPostId()).orElseThrow();
        assertThat(updated.getPostStatus()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void delete_hardDelete_removesRow() throws Exception {
        User author = userRepository.findByUsername("manager").orElseThrow();
        ContentPost existing = ContentPost.builder()
                .author(author)
                .postTitle("T")
                .postContent("C")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        existing = contentPostRepository.save(existing);

        mockMvc.perform(delete("/api/v1/manager/posts/{id}", existing.getPostId())
                        .param("hard", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(contentPostRepository.existsById(existing.getPostId())).isFalse();
    }
}
