package org.szepietowski.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.szepietowski.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
