package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_content_posts")
public class ContentPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    Long postId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    User author;

    @Size(max = 500)
    @NotNull
    @Column(name = "post_title", nullable = false, length = 500)
    String postTitle;

    @NotNull
    @Lob
    @Column(name = "post_content", nullable = false)
    String postContent;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "post_category", nullable = false, length = 100)
    PostCategory postCategory;

    @ElementCollection(targetClass = PostTag.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    Set<PostTag> tags = new HashSet<>();

    @Size(max = 1000)
    @Column(name = "featured_image_url", length = 1000)
    String featuredImageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "post_status", nullable = false, length = 50)
    PostStatus postStatus = PostStatus.DRAFT;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    Long viewCount;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    Integer likeCount = 0;

    @Builder.Default
    @Column(name = "share_count", nullable = false)
    Integer shareCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;
}