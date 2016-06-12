package org.szepietowski.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.szepietowski.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
