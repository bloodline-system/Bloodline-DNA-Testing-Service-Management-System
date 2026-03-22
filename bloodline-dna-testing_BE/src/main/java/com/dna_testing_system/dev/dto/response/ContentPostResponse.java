package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContentPostResponse {
    Long postId;
    LocalDateTime createdAt;
    String featuredImageUrl;
    Integer likeCount;
    PostCategory postCategory;
    String postContent;
    PostStatus postStatus;
    String postTitle;
    LocalDateTime publishedAt;
    Integer shareCount;
    LocalDateTime updatedAt;
    Long viewCount;
    Long authorId;
    Set<PostTag> tags;
}
