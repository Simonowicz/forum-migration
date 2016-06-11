package org.szepietowski.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.szepietowski.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
