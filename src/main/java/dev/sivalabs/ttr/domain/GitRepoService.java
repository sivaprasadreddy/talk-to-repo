package dev.sivalabs.ttr.domain;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GitRepoService {

    private static final Logger log = LoggerFactory.getLogger(GitRepoService.class);

    private final GitRepoRepository gitRepoRepository;

    public GitRepoService(GitRepoRepository gitRepoRepository) {
        this.gitRepoRepository = gitRepoRepository;
    }

    public GitRepo cloneRepo(String repoUrl) {
        Optional<GitRepo> existing = gitRepoRepository.findByRepoUrl(repoUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        String repoName = extractRepoName(repoUrl);
        Path localPath = Path.of(System.getProperty("user.home"), "ttr", repoName);

        try {
            Files.createDirectories(localPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + localPath.getParent(), e);
        }

        try (Git _ = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath.toFile())
                .call()) {
            log.info("Cloned {} to {}", repoUrl, localPath);
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to clone repository: " + e.getMessage(), e);
        }

        GitRepo repo = new GitRepo();
        repo.setRepoUrl(repoUrl);
        repo.setRepoName(repoName);
        repo.setLocalPath(localPath.toString());
        repo.setClonedAt(LocalDateTime.now());
        return gitRepoRepository.save(repo);
    }

    @Transactional(readOnly = true)
    public List<GitRepo> findAll() {
        return gitRepoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public GitRepo findById(Long id) {
        return gitRepoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + id));
    }

    public void pullRepo(Long id) {
        GitRepo repo = findById(id);
        Path localPath = Path.of(repo.getLocalPath());
        try (Git git = Git.open(localPath.toFile())) {
            git.pull().call();
            log.info("Pulled latest changes for repo {} at {}", repo.getRepoName(), localPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open repository at " + localPath + ": " + e.getMessage(), e);
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to pull repository: " + e.getMessage(), e);
        }
    }

    private String extractRepoName(String repoUrl) {
        String path = URI.create(repoUrl).getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }
}
