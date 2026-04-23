package com.tabularhub.service;

import com.tabularhub.api.dto.CommitRequest;
import com.tabularhub.api.dto.CommitResponse;
import com.tabularhub.api.dto.CreateRepoRequest;
import com.tabularhub.api.dto.RepoResponse;
import com.tabularhub.config.HubProperties;
import com.tabularhub.domain.RepoRecord;
import com.tabularhub.domain.RepoRecordRepository;
import com.tabularhub.engine.TvcsRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepoService {

  private final HubProperties hubProperties;
  private final RepoRecordRepository repoRecordRepository;
  private final TvcsRunner tvcsRunner;

  public RepoService(HubProperties hubProperties, RepoRecordRepository repoRecordRepository, TvcsRunner tvcsRunner) {
    this.hubProperties = hubProperties;
    this.repoRecordRepository = repoRecordRepository;
    this.tvcsRunner = tvcsRunner;
  }

  @Transactional(readOnly = true)
  public List<RepoResponse> list() {
    return repoRecordRepository.findAll().stream()
        .sorted(Comparator.comparing(RepoRecord::getCreatedAt).reversed())
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public RepoResponse get(String id) {
    RepoRecord r =
        repoRecordRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repo not found"));
    return toResponse(r);
  }

  @Transactional
  public RepoResponse create(CreateRepoRequest req) {
    if (req.getName() == null || req.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
    }
    String id = UUID.randomUUID().toString();
    Path reposRoot = hubProperties.getReposRoot().toAbsolutePath().normalize();
    Path repoPath = reposRoot.resolve(id).normalize();
    try {
      Files.createDirectories(reposRoot);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cannot create repos root");
    }
    String slug = allocateSlug(req.getName());
    tvcsRunner.run(Path.of(".").toAbsolutePath(), List.of("init", repoPath.toString()));

    RepoRecord row = new RepoRecord();
    row.setId(id);
    row.setName(req.getName().trim());
    row.setSlug(slug);
    row.setFilesystemPath(repoPath.toString());
    row.setCreatedAt(Instant.now());
    repoRecordRepository.save(row);
    return toResponse(row);
  }

  public CommitResponse commit(String repoId, CommitRequest req) {
    RepoRecord row =
        repoRecordRepository
            .findById(repoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repo not found"));
    if (req.getMessage() == null || req.getMessage().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message required");
    }
    if (req.getTables() == null || req.getTables().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tables required");
    }
    Path root = Path.of(row.getFilesystemPath());
    try {
      Path staging = Files.createTempDirectory("tabularhub-staging-");
      try {
        for (var e : req.getTables().entrySet()) {
          String name = e.getKey();
          if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "table keys must end with .csv");
          }
          Files.writeString(staging.resolve(name), e.getValue(), StandardCharsets.UTF_8);
        }
        String out =
            tvcsRunner.run(
                Path.of(".").toAbsolutePath(),
                List.of("commit", root.toString(), req.getMessage().trim(), staging.toString()));
        String lastLine = out.lines().reduce((a, b) -> b).orElse("");
        return new CommitResponse(lastLine.trim());
      } finally {
          deleteRecursive(staging);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "staging failed");
    }
  }

  public String log(String repoId) {
    RepoRecord row =
        repoRecordRepository
            .findById(repoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repo not found"));
    Path root = Path.of(row.getFilesystemPath());
    return tvcsRunner.run(Path.of(".").toAbsolutePath(), List.of("log", root.toString()));
  }

  public String head(String repoId) {
    RepoRecord row =
        repoRecordRepository
            .findById(repoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repo not found"));
    Path root = Path.of(row.getFilesystemPath());
    return tvcsRunner.run(Path.of(".").toAbsolutePath(), List.of("head", root.toString()));
  }

  private RepoResponse toResponse(RepoRecord r) {
    return new RepoResponse(r.getId(), r.getName(), r.getSlug(), r.getFilesystemPath(), r.getCreatedAt());
  }

  private String allocateSlug(String name) {
    String base =
        name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    if (base.isEmpty()) {
      base = "repo";
    }
    String candidate = base;
    int i = 0;
    while (repoRecordRepository.existsBySlug(candidate)) {
      i++;
      candidate = base + "-" + i;
    }
    return candidate;
  }

  private static void deleteRecursive(Path p) throws IOException {
    if (Files.notExists(p)) {
      return;
    }
    try (var walk = Files.walk(p)) {
      var paths = walk.sorted(Comparator.reverseOrder()).toList();
      for (Path x : paths) {
        Files.deleteIfExists(x);
      }
    }
  }
}
