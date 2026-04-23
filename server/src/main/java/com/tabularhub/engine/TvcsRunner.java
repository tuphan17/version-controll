package com.tabularhub.engine;

import com.tabularhub.config.HubProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TvcsRunner {

  private final HubProperties hubProperties;
  private final EmbeddedTvcs embeddedTvcs;

  public TvcsRunner(HubProperties hubProperties, EmbeddedTvcs embeddedTvcs) {
    this.hubProperties = hubProperties;
    this.embeddedTvcs = embeddedTvcs;
  }

  public String run(Path workingDirectory, List<String> args) {
    if (useEmbeddedEngine()) {
      return embeddedTvcs.run(args);
    }
    return runNative(workingDirectory, args);
  }

  private boolean useEmbeddedEngine() {
    String mode =
        hubProperties.getTvcsMode() == null
            ? "auto"
            : hubProperties.getTvcsMode().trim().toLowerCase(Locale.ROOT);
    if ("embedded".equals(mode)) {
      return true;
    }
    if ("native".equals(mode)) {
      return false;
    }
    return !nativeExecutableResolved();
  }

  private boolean nativeExecutableResolved() {
    String cmd = hubProperties.getTvcsExecutable();
    if (cmd == null || cmd.isBlank()) {
      return false;
    }
    Path p = Path.of(cmd);
    if (p.isAbsolute()) {
      return Files.isRegularFile(p);
    }
    String pathEnv = System.getenv("PATH");
    if (pathEnv == null || pathEnv.isBlank()) {
      return false;
    }
    for (String dir : pathEnv.split(Pattern.quote(java.io.File.pathSeparator))) {
      if (dir.isBlank()) {
        continue;
      }
      Path base = Path.of(dir);
      if (Files.isRegularFile(base.resolve(cmd))) {
        return true;
      }
      if (Files.isRegularFile(base.resolve(cmd + ".exe"))) {
        return true;
      }
    }
    return false;
  }

  private String runNative(Path workingDirectory, List<String> args) {
    List<String> command = new ArrayList<>();
    command.add(hubProperties.getTvcsExecutable());
    command.addAll(args);
    ProcessBuilder pb = new ProcessBuilder(command);
    if (workingDirectory != null) {
      pb.directory(workingDirectory.toFile());
    }
    pb.redirectErrorStream(true);
    try {
      Process p = pb.start();
      byte[] out = p.getInputStream().readAllBytes();
      boolean finished = p.waitFor(60, TimeUnit.SECONDS);
      if (!finished) {
        p.destroyForcibly();
        throw new TvcsException("tvcs timed out", -1, "");
      }
      String text = new String(out, StandardCharsets.UTF_8).trim();
      int code = p.exitValue();
      if (code != 0) {
        throw new TvcsException("tvcs failed: " + text, code, text);
      }
      return text;
    } catch (IOException e) {
      throw new TvcsException(e.getMessage() == null ? "tvcs io error" : e.getMessage(), -1, "");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TvcsException("interrupted", -1, "");
    }
  }
}
