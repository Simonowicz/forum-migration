package org.szepietowski.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.szepietowski.entity.ScrapingMetadata;

public interface ScrapingMetadataRepository extends JpaRepository<ScrapingMetadata, Long> {
    ScrapingMetadata findByRunner(String runner);
}
