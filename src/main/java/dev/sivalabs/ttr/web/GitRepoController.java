package dev.sivalabs.ttr.web;

import dev.sivalabs.ttr.domain.GitRepo;
import dev.sivalabs.ttr.domain.GitRepoService;
import dev.sivalabs.ttr.domain.ReadmeGenerationService;
import dev.sivalabs.ttr.domain.RepoIngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GitRepoController {

    private static final Logger log = LoggerFactory.getLogger(GitRepoController.class);

    private final GitRepoService gitRepoService;
    private final RepoIngestionService repoIngestionService;
    private final ReadmeGenerationService readmeGenerationService;

    public GitRepoController(GitRepoService gitRepoService,
                             RepoIngestionService repoIngestionService,
                             ReadmeGenerationService readmeGenerationService) {
        this.gitRepoService = gitRepoService;
        this.repoIngestionService = repoIngestionService;
        this.readmeGenerationService = readmeGenerationService;
    }

    @GetMapping({"/", "/repos"})
    public String listRepos(Model model) {
        log.debug("Listing all repositories");
        model.addAttribute("repos", gitRepoService.findAll());
        return "repos";
    }

    @GetMapping("/add-repo")
    public String showAddRepoForm(Model model) {
        model.addAttribute("addRepoForm", new AddRepoForm());
        return "add-repo";
    }

    @PostMapping("/add-repo")
    public String addRepo(@Valid @ModelAttribute AddRepoForm addRepoForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "add-repo";
        }
        log.info("Adding repository: {}", addRepoForm.getRepoUrl());
        try {
            GitRepo repo = gitRepoService.cloneRepo(addRepoForm.getRepoUrl());
            log.info("Repository added successfully: {} (id={})", repo.getRepoName(), repo.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Repository cloned successfully!");
            return "redirect:/explore-repo/" + repo.getId();
        } catch (Exception e) {
            log.error("Failed to add repository {}", addRepoForm.getRepoUrl(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return "add-repo";
        }
    }

    @GetMapping("/explore-repo/{id}")
    public String exploreRepo(@PathVariable Long id, Model model) {
        log.debug("Exploring repo id={}", id);
        GitRepo repo = gitRepoService.findById(id);
        model.addAttribute("repo", repo);
        return "explore-repo";
    }

    @PostMapping("/repos/{id}/pull")
    public String pullRepo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Pull requested for repo id={}", id);
        try {
            gitRepoService.pullRepo(id);
            log.info("Pull completed for repo id={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Repository refreshed successfully!");
        } catch (Exception e) {
            log.error("Failed to pull repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to refresh repository: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }

    @PostMapping("/repos/{id}/reingest")
    public String reingestRepo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Re-ingest requested for repo id={}", id);
        try {
            GitRepo repo = gitRepoService.findById(id);
            repoIngestionService.reingest(repo);
            log.info("Re-ingest completed for repo id={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Repository re-ingested successfully!");
        } catch (Exception e) {
            log.error("Failed to re-ingest repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to re-ingest repository: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }

    @PostMapping("/repos/{id}/generate-readme")
    public String generateReadme(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("README generation requested for repo id={}", id);
        try {
            GitRepo repo = gitRepoService.findById(id);
            repoIngestionService.ingestIfNeeded(repo);
            readmeGenerationService.generateReadme(repo);
            log.info("README generation completed for repo id={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "README generated successfully!");
        } catch (Exception e) {
            log.error("Failed to generate README for repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate README: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }
}
