package com.tabularhub.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoRecordRepository extends JpaRepository<RepoRecord, String> {

  boolean existsBySlug(String slug);
}
