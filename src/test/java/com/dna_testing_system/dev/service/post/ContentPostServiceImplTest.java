package com.dna_testing_system.dev.service.post;

import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.request.ContentPostRequest;
import com.dna_testing_system.dev.dto.response.ContentPostResponse;
import com.dna_testing_system.dev.entity.ContentPost;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import com.dna_testing_system.dev.repository.ContentPostRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.impl.ContentPostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPostServiceImplTest {

    @Mock
    ContentPostRepository contentPostRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ContentPostServiceImpl service;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "N/A");
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllPostsMapsNullTagsToEmptySetAndNullAuthorToNullAuthorId() {
        ContentPost post = ContentPost.builder()
                .postId(1L)
                .postTitle("t")
                .postContent("c")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .build();
        post.setTags(null);
        post.setAuthor(null);

        when(contentPostRepository.findAll()).thenReturn(List.of(post));

        List<ContentPostResponse> result = service.getAllPosts();

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getTags());
        assertTrue(result.getFirst().getTags().isEmpty());
        assertNull(result.getFirst().getAuthorId());
    }

    @Test
    void getPostByIdThrowsWhenNotFound() {
        when(contentPostRepository.findById(10L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> service.getPostById(10L));
        assertTrue(ex.getMessage().contains("Not found post with id: 10"));
    }

    @Test
    void getPostsPageMapsPageResponseCorrectly() {
        ContentPost post = ContentPost.builder()
                .postId(1L)
                .postTitle("t")
                .postContent("c")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .tags(Set.of(PostTag.HEALTH))
                .build();

        Pageable pageable = PageRequest.of(1, 2);
        Page<ContentPost> page = new PageImpl<>(List.of(post), pageable, 5);

        when(contentPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<ContentPostResponse> result = service.getPostsPage("q", PostStatus.DRAFT, PostCategory.NEWS, PostTag.HEALTH, pageable);

        assertEquals(1, result.getPageNumber());
        assertEquals(2, result.getPageSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isLast());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().getFirst().getPostId());
        assertTrue(result.getContent().getFirst().getTags().contains(PostTag.HEALTH));
    }

    @Test
    void createPostDefaultsStatusToDraftAndNormalizesTagsAndCounts() {
        User author = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));

        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost saved = inv.getArgument(0);
            saved.setPostId(100L);
            return saved;
        });

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.NEWS)
                .tags(null)
                .postStatus(null)
                .build();

        ContentPostResponse response = service.createPost(request);

        assertEquals(100L, response.getPostId());
        assertEquals(PostStatus.DRAFT, response.getPostStatus());
        assertEquals("u1", response.getAuthorId());
        assertNotNull(response.getTags());
        assertTrue(response.getTags().isEmpty());
        assertEquals(0L, response.getViewCount());
        assertEquals(0, response.getLikeCount());
        assertEquals(0, response.getShareCount());

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        ContentPost saved = captor.getValue();
        assertEquals(PostStatus.DRAFT, saved.getPostStatus());
        assertNotNull(saved.getTags());
        assertTrue(saved.getTags().isEmpty());
        assertEquals(0L, saved.getViewCount());
        assertEquals(0, saved.getLikeCount());
        assertEquals(0, saved.getShareCount());
        assertNotNull(saved.getCreatedAt());
        assertEquals("Title", saved.getPostTitle());
        assertEquals("Content", saved.getPostContent());
        assertEquals(PostCategory.NEWS, saved.getPostCategory());
    }

    @Test
    void createPostSetsPublishedAtWhenStatusPublished() {
        User author = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));

        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost saved = inv.getArgument(0);
            saved.setPostId(101L);
            return saved;
        });

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.PUBLISHED)
                .build();

        ContentPostResponse response = service.createPost(request);

        assertEquals(PostStatus.PUBLISHED, response.getPostStatus());
        assertNotNull(response.getPublishedAt());

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertNotNull(captor.getValue().getPublishedAt());
    }

    @Test
    void createPostThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.NEWS)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createPost(request));
        assertTrue(ex.getMessage().contains("User not found with username: alice"));

        verify(contentPostRepository, never()).save(any());
    }

    @Test
    void updatePostReturningKeepsCurrentStatusWhenRequestStatusNullAndNormalizesNullCounts() {
        ContentPost existing = ContentPost.builder()
                .postId(5L)
                .postTitle("old")
                .postContent("old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.ARCHIVED)
                .viewCount(null)
                .likeCount(null)
                .shareCount(null)
                .publishedAt(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(contentPostRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("new")
                .postContent("new")
                .postCategory(PostCategory.NEWS)
                .postStatus(null)
                .tags(null)
                .build();

        ContentPostResponse response = service.updatePostReturning(5L, request);

        assertEquals(PostStatus.ARCHIVED, response.getPostStatus());
        assertEquals(0L, response.getViewCount());
        assertEquals(0, response.getLikeCount());
        assertEquals(0, response.getShareCount());
        assertNotNull(response.getUpdatedAt());

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        ContentPost saved = captor.getValue();
        assertEquals(PostStatus.ARCHIVED, saved.getPostStatus());
        assertEquals(0L, saved.getViewCount());
        assertEquals(0, saved.getLikeCount());
        assertEquals(0, saved.getShareCount());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getTags());
        assertTrue(saved.getTags().isEmpty());
    }

    @Test
    void updatePostReturningSetsPublishedAtWhenBecomesPublishedAndPublishedAtNull() {
        ContentPost existing = ContentPost.builder()
                .postId(6L)
                .postTitle("old")
                .postContent("old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .publishedAt(null)
                .build();

        when(contentPostRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("new")
                .postContent("new")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.PUBLISHED)
                .build();

        ContentPostResponse response = service.updatePostReturning(6L, request);

        assertEquals(PostStatus.PUBLISHED, response.getPostStatus());
        assertNotNull(response.getPublishedAt());
    }

    @Test
    void updatePostReturningThrowsWhenNotFound() {
        when(contentPostRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.updatePostReturning(55L, ContentPostRequest.builder().postTitle("t").postContent("c").postCategory(PostCategory.NEWS).build()));
    }

    @Test
    void updatePostStatusThrowsWhenStatusNull() {
        assertThrows(IllegalArgumentException.class, () -> service.updatePostStatus(1L, null));
    }

    @Test
    void updatePostStatusSetsPublishedAtWhenPublishedAndPublishedAtNull() {
        ContentPost existing = ContentPost.builder()
                .postId(1L)
                .postTitle("t")
                .postContent("c")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .publishedAt(null)
                .build();

        when(contentPostRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostResponse response = service.updatePostStatus(1L, PostStatus.PUBLISHED);

        assertEquals(PostStatus.PUBLISHED, response.getPostStatus());
        assertNotNull(response.getPublishedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void softDeletePostMarksDeletedAndSaves() {
        ContentPost existing = ContentPost.builder()
                .postId(2L)
                .postTitle("t")
                .postContent("c")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.PUBLISHED)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .build();

        when(contentPostRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        service.softDeletePost(2L);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertEquals(PostStatus.DELETED, captor.getValue().getPostStatus());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void deletePostThrowsWhenNotExists() {
        when(contentPostRepository.existsById(9L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.deletePost(9L));
        verify(contentPostRepository, never()).deleteById(anyLong());
    }

    @Test
    void deletePostDeletesWhenExists() {
        when(contentPostRepository.existsById(9L)).thenReturn(true);

        service.deletePost(9L);

        verify(contentPostRepository).deleteById(9L);
    }
}
