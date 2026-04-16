package dev.sivalabs.ttr.web;

import dev.sivalabs.ttr.domain.GitRepo;
import dev.sivalabs.ttr.domain.GitRepoService;
import dev.sivalabs.ttr.domain.RepoChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RepoChatController {

    private static final Logger log = LoggerFactory.getLogger(RepoChatController.class);

    private final GitRepoService gitRepoService;
    private final RepoChatService repoChatService;

    public RepoChatController(GitRepoService gitRepoService, RepoChatService repoChatService) {
        this.gitRepoService = gitRepoService;
        this.repoChatService = repoChatService;
    }

    @PostMapping("/repos/{id}/chat")
    public ResponseEntity<ChatResponse> chat(@PathVariable Long id, @RequestBody ChatRequest request) {
        log.info("Chat request for repo id={}: {}", id, request.question());
        GitRepo repo = gitRepoService.findById(id);
        if (repo.getIngestedAt() == null) {
            log.warn("Chat attempted on non-ingested repo id={}", id);
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Repository has not been ingested yet. Please click 'Ingest' first."));
        }
        String answer = repoChatService.chat(repo, request.question());
        log.debug("Chat response for repo id={} ({} chars)", id, answer != null ? answer.length() : 0);
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    public record ChatRequest(String question) {}

    public record ChatResponse(String answer) {}
}
