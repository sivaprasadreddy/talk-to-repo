package dev.sivalabs.ttr.web;

import dev.sivalabs.ttr.domain.GitRepo;
import dev.sivalabs.ttr.domain.GitRepoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GitRepoController {

    private final GitRepoService gitRepoService;

    public GitRepoController(GitRepoService gitRepoService) {
        this.gitRepoService = gitRepoService;
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
}
