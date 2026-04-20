package com.dna_testing_system.dev.repository.specification;

import com.dna_testing_system.dev.entity.ContentPost;
import com.dna_testing_system.dev.enums.PostCategory;
import com.dna_testing_system.dev.enums.PostStatus;
import com.dna_testing_system.dev.enums.PostTag;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public final class ContentPostSpecifications {

    private ContentPostSpecifications() {
    }

    public static Specification<ContentPost> search(String query) {
        return (root, criteriaQuery, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }

            String q = "%" + query.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("postTitle")), q);
        };
    }

    public static Specification<ContentPost> hasStatus(PostStatus status) {
        return (root, criteriaQuery, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("postStatus"), status);
    }

    public static Specification<ContentPost> hasCategory(PostCategory category) {
        return (root, criteriaQuery, cb) -> category == null
                ? cb.conjunction()
                : cb.equal(root.get("postCategory"), category);
    }

    public static Specification<ContentPost> hasTag(PostTag tag) {
        return (root, criteriaQuery, cb) -> {
            if (tag == null) {
                return cb.conjunction();
            }

            criteriaQuery.distinct(true);
            Join<Object, Object> tagsJoin = root.join("tags", JoinType.LEFT);
            return cb.equal(tagsJoin, tag);
        };
    }
}
