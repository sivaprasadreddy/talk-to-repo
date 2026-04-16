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
        try {
            GitRepo repo = gitRepoService.cloneRepo(addRepoForm.getRepoUrl());
            redirectAttributes.addFlashAttribute("successMessage", "Repository cloned successfully!");
            return "redirect:/explore-repo/" + repo.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "add-repo";
        }
    }

    @GetMapping("/explore-repo/{id}")
    public String exploreRepo(@PathVariable Long id, Model model) {
        GitRepo repo = gitRepoService.findById(id);
        model.addAttribute("repo", repo);
        return "explore-repo";
    }

    @PostMapping("/repos/{id}/pull")
    public String pullRepo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            gitRepoService.pullRepo(id);
            redirectAttributes.addFlashAttribute("successMessage", "Repository refreshed successfully!");
        } catch (Exception e) {
            log.error("Failed to pull repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to refresh repository: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }

    @PostMapping("/repos/{id}/reingest")
    public String reingestRepo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            GitRepo repo = gitRepoService.findById(id);
            repoIngestionService.reingest(repo);
            redirectAttributes.addFlashAttribute("successMessage", "Repository re-ingested successfully!");
        } catch (Exception e) {
            log.error("Failed to re-ingest repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to re-ingest repository: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }

    @PostMapping("/repos/{id}/generate-readme")
    public String generateReadme(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            GitRepo repo = gitRepoService.findById(id);
            repoIngestionService.ingestIfNeeded(repo);
            readmeGenerationService.generateReadme(repo);
            redirectAttributes.addFlashAttribute("successMessage", "README generated successfully!");
        } catch (Exception e) {
            log.error("Failed to generate README for repo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate README: " + e.getMessage());
        }
        return "redirect:/explore-repo/" + id;
    }
}
