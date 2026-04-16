package dev.sivalabs.ttr.domain;

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

        runGitClone(repoUrl, localPath);

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

    private void runGitClone(String repoUrl, Path localPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "clone", repoUrl, localPath.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("git clone failed (exit " + exitCode + "): " + output);
            }
            log.info("Cloned {} to {}", repoUrl, localPath);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run git clone: " + e.getMessage(), e);
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
