package dev.sivalabs.ttr.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AddRepoForm {

    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "https?://[^\\s/$.?#].[^\\s]*",
        message = "Please enter a valid repository URL"
    )
    private String repoUrl;

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
}
