package dev.sivalabs.ttr.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitRepoRepository extends JpaRepository<GitRepo, Long> {
    Optional<GitRepo> findByRepoUrl(String repoUrl);
}
