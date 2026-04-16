package dev.sivalabs.ttr.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repositories")
public class GitRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String repoUrl;

    @Column(nullable = false)
    private String repoName;

    @Column(nullable = false)
    private String localPath;

    @Column(nullable = false)
    private LocalDateTime clonedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public LocalDateTime getClonedAt() { return clonedAt; }
    public void setClonedAt(LocalDateTime clonedAt) { this.clonedAt = clonedAt; }
}
