package com.dna_testing_system.dev.service.post;

import com.dna_testing_system.dev.dto.request.ContentPostRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPostServiceImplBvaTest {

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
    void tearDownSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== createPost BVA/edge ====================

    @Test
    void createPost_statusNull_defaultsToDraft_andPublishedAtNull_andTagsEmpty() {
        User author = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost p = inv.getArgument(0);
            p.setPostId(1L);
            return p;
        });

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(null)
                .tags(null)
                .build();

        var response = service.createPost(request);

        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getPostStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.getTags()).isNotNull().isEmpty();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getAuthorId()).isEqualTo("u1");

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        ContentPost saved = captor.getValue();
        assertThat(saved.getPostStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(saved.getTags()).isNotNull().isEmpty();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getViewCount()).isEqualTo(0L);
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getShareCount()).isEqualTo(0);
        assertThat(saved.getAuthor()).isSameAs(author);
    }

    @Test
    void createPost_statusPublished_setsPublishedAt() {
        User author = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(PostStatus.PUBLISHED)
                .tags(Set.of(PostTag.DNA_TESTING))
                .build();

        service.createPost(request);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
        assertThat(captor.getValue().getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    void createPost_tagsEmpty_defaultsToEmptySet() {
        User author = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .tags(Set.of())
                .build();

        service.createPost(request);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getTags()).isNotNull().isEmpty();
    }

    @Test
    void createPost_userNotFound_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("Title")
                .postContent("Content")
                .postCategory(PostCategory.TESTING_GUIDE)
                .build();

        assertThatThrownBy(() -> service.createPost(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(contentPostRepository);
    }

    // ==================== updatePostReturning BVA/edge ====================

    @Test
    void updatePostReturning_requestStatusNull_keepsExistingStatus() {
        ContentPost existing = ContentPost.builder()
                .postId(5L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.ARCHIVED)
                .viewCount(1L)
                .likeCount(1)
                .shareCount(1)
                .publishedAt(null)
                .build();

        when(contentPostRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("New")
                .postContent("New")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(null)
                .tags(Set.of(PostTag.TIPS))
                .build();

        var response = service.updatePostReturning(5L, request);

        assertThat(response.getPostId()).isEqualTo(5L);
        assertThat(response.getPostStatus()).isEqualTo(PostStatus.ARCHIVED);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getPostStatus()).isEqualTo(PostStatus.ARCHIVED);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void updatePostReturning_whenCountersNull_defaultsToZero() {
        ContentPost existing = ContentPost.builder()
                .postId(6L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(null)
                .likeCount(null)
                .shareCount(null)
                .build();

        when(contentPostRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("New")
                .postContent("New")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(null)
                .build();

        service.updatePostReturning(6L, request);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        ContentPost saved = captor.getValue();
        assertThat(saved.getViewCount()).isEqualTo(0L);
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getShareCount()).isEqualTo(0);
    }

    @Test
    void updatePostReturning_switchToPublished_setsPublishedAtWhenNull() {
        ContentPost existing = ContentPost.builder()
                .postId(7L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .publishedAt(null)
                .build();

        when(contentPostRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("New")
                .postContent("New")
                .postCategory(PostCategory.TESTING_GUIDE)
                .postStatus(PostStatus.PUBLISHED)
                .build();

        service.updatePostReturning(7L, request);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void updatePostReturning_notFound_throwsNoSuchElementException() {
        when(contentPostRepository.findById(404L)).thenReturn(Optional.empty());

        ContentPostRequest request = ContentPostRequest.builder()
                .postTitle("New")
                .postContent("New")
                .postCategory(PostCategory.TESTING_GUIDE)
                .build();

        assertThatThrownBy(() -> service.updatePostReturning(404L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Not found post with id: 404");

        verify(contentPostRepository, never()).save(any());
    }

    // ==================== updatePostStatus BVA/edge ====================

    @Test
    void updatePostStatus_statusNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.updatePostStatus(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(contentPostRepository);
    }

    @Test
    void updatePostStatus_publish_setsPublishedAtWhenNull() {
        ContentPost existing = ContentPost.builder()
                .postId(8L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .publishedAt(null)
                .build();

        when(contentPostRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updatePostStatus(8L, PostStatus.PUBLISHED);

        assertThat(response.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void updatePostStatus_whenPublishedAtAlreadySet_doesNotOverrideIt() {
        LocalDateTime existingPublishedAt = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        ContentPost existing = ContentPost.builder()
                .postId(81L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.PUBLISHED)
                .publishedAt(existingPublishedAt)
                .build();

        when(contentPostRepository.findById(81L)).thenReturn(Optional.of(existing));
        when(contentPostRepository.save(any(ContentPost.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updatePostStatus(81L, PostStatus.PUBLISHED);

        ArgumentCaptor<ContentPost> captor = ArgumentCaptor.forClass(ContentPost.class);
        verify(contentPostRepository).save(captor.capture());
        assertThat(captor.getValue().getPublishedAt()).isEqualTo(existingPublishedAt);
    }

    @Test
    void updatePostStatus_notFound_throwsNoSuchElementException() {
        when(contentPostRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePostStatus(404L, PostStatus.DRAFT))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Not found post with id: 404");

        verify(contentPostRepository, never()).save(any());
    }

    // ==================== softDeletePost / deletePost ====================

    @Test
    void softDeletePost_setsStatusDeleted_andSaves() {
        ContentPost existing = ContentPost.builder()
                .postId(9L)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .postTitle("Old")
                .postContent("Old")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .viewCount(0L)
                .likeCount(0)
                .shareCount(0)
                .build();

        when(contentPostRepository.findById(9L)).thenReturn(Optional.of(existing));

        service.softDeletePost(9L);

        verify(contentPostRepository).save(existing);
        assertThat(existing.getPostStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    void softDeletePost_notFound_throwsNoSuchElementException() {
        when(contentPostRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDeletePost(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Not found post with id: 404");

        verify(contentPostRepository, never()).save(any());
    }

    @Test
    void deletePost_notExists_throwsNoSuchElementException() {
        when(contentPostRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.deletePost(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Not found post");

        verify(contentPostRepository, never()).deleteById(anyLong());
    }

    @Test
    void deletePost_exists_deletesById() {
        when(contentPostRepository.existsById(10L)).thenReturn(true);

        service.deletePost(10L);

        verify(contentPostRepository).deleteById(10L);
    }

    // ==================== read-only methods ====================

    @Test
    void getPostById_notFound_throwsNoSuchElementException() {
        when(contentPostRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPostById(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Not found post with id: 404");
    }

    @Test
    void getAllPosts_mapsTagsNullToEmptySet() {
        ContentPost post = ContentPost.builder()
                .postId(1L)
                .postTitle("T")
                .postContent("C")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .tags(null)
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .build();

        when(contentPostRepository.findAll()).thenReturn(List.of(post));

        var responses = service.getAllPosts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTags()).isNotNull().isEmpty();
        assertThat(responses.get(0).getAuthorId()).isEqualTo("u1");
    }

    @Test
    void getPostsPage_mapsPageToPageResponse() {
        ContentPost post = ContentPost.builder()
                .postId(1L)
                .postTitle("T")
                .postContent("C")
                .postCategory(PostCategory.NEWS)
                .postStatus(PostStatus.DRAFT)
                .tags(Set.of(PostTag.TIPS))
                .author(User.builder().id("u1").username("alice").passwordHash("x").build())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(contentPostRepository.findAll(org.mockito.ArgumentMatchers.<Specification<ContentPost>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));

        var page = service.getPostsPage("q", PostStatus.DRAFT, PostCategory.NEWS, PostTag.TIPS, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getPageNumber()).isEqualTo(0);
        assertThat(page.getPageSize()).isEqualTo(10);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.isLast()).isTrue();
    }
}
