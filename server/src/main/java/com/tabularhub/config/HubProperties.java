package com.tabularhub.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tabularhub")
public class HubProperties {

  private Path reposRoot = Path.of("./data/repos");
  private String tvcsExecutable = "tvcs";
  // auto | native | embedded
  private String tvcsMode = "auto";

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

  public String getTvcsMode() {
    return tvcsMode;
  }

  public void setTvcsMode(String tvcsMode) {
    this.tvcsMode = tvcsMode;
  }
}
