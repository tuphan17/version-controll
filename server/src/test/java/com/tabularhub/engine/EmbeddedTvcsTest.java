package com.tabularhub.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmbeddedTvcsTest {

  private final EmbeddedTvcs tvcs = new EmbeddedTvcs();

  @Test
  void roundTrip(@TempDir Path tmp) throws Exception {
    Path repo = tmp.resolve("r");
    tvcs.run(List.of("init", repo.toString()));

    Path staging = tmp.resolve("st");
    Files.createDirectories(staging);
    Files.writeString(staging.resolve("t.csv"), "a,b\n1,2\n", StandardCharsets.UTF_8);

    String hash = tvcs.run(List.of("commit", repo.toString(), "first", staging.toString()));
    assertThat(hash).hasSize(16).matches("[0-9a-f]+");

    assertThat(tvcs.run(List.of("head", repo.toString()))).startsWith("main ");

    String log = tvcs.run(List.of("log", repo.toString()));
    assertThat(log).contains("commit " + hash).contains("first");
  }
}
