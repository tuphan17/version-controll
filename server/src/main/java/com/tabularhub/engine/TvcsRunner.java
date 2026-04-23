package com.tabularhub.engine;

import com.tabularhub.config.HubProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class TvcsRunner {

  private final HubProperties hubProperties;

  public TvcsRunner(HubProperties hubProperties) {
    this.hubProperties = hubProperties;
  }

  public String run(Path workingDirectory, List<String> args) {
    List<String> cmd = new ArrayList<>();
    cmd.add(hubProperties.getTvcsExecutable());
    cmd.addAll(args);
    ProcessBuilder pb = new ProcessBuilder(cmd);
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
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TvcsException(e.getMessage(), -1, "");
    }
  }
}
