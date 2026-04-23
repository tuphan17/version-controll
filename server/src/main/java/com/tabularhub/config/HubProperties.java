package com.tabularhub.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tabularhub")
public class HubProperties {

  /** Root directory containing one folder per repository. */
  private Path reposRoot = Path.of("./data/repos");

  /** tvcs binary name or absolute path. */
  private String tvcsExecutable = "tvcs";

  /**
   * {@code auto} — use native tvcs if the executable resolves; otherwise pure Java. {@code native}
   * — require native binary. {@code embedded} — always pure Java.
   */
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
