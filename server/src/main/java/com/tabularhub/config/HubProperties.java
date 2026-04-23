package com.tabularhub.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tabularhub")
public class HubProperties {

  /** Root directory containing one folder per repository. */
  private Path reposRoot = Path.of("./data/repos");

  /** tvcs binary name or absolute path. */
  private String tvcsExecutable = "tvcs";

  public Path getReposRoot() {
    return reposRoot;
  }

  public void setReposRoot(Path reposRoot) {
    this.reposRoot = reposRoot;
  }

  public String getTvcsExecutable() {
    return tvcsExecutable;
  }

  public void setTvcsExecutable(String tvcsExecutable) {
    this.tvcsExecutable = tvcsExecutable;
  }
}
