package org.szepietowski.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.szepietowski.entity.Forum;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {
}
