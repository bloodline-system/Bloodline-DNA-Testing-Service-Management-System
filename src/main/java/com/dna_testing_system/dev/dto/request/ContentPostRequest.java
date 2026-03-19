package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContentPostRequest {
    @NotNull
    Long authorId;

    Long postId;

    @NotBlank
    @Size(max = 500)
    String postTitle;

    @NotBlank
    String postContent;

    @Size(max = 1000)
    String featuredImageUrl;

    @NotNull
    PostCategory postCategory;

    Set<PostTag> tags;

    PostStatus postStatus;
}
