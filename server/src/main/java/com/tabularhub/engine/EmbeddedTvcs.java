package com.tabularhub.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmbeddedTvcs {

  private static final int HASH_LEN = 16;

  public String run(List<String> args) {
    if (args.isEmpty()) {
      throw new TvcsException("missing subcommand", -1, "");
    }
    try {
      return switch (args.get(0)) {
        case "init" -> cmdInit(args);
        case "commit" -> cmdCommit(args);
        case "log" -> cmdLog(args);
        case "head" -> cmdHead(args);
        default -> throw new TvcsException("huh? " + args.get(0), -1, "");
      };
    } catch (IOException e) {
      throw new TvcsException(e.getMessage(), -1, e.toString());
    }
  }

  private String cmdInit(List<String> args) throws IOException {
    if (args.size() < 2) {
      throw new TvcsException("usage: tvcs init <repo_dir>", -1, "");
    }
    Path repo = Path.of(args.get(1)).toAbsolutePath().normalize();
    Files.createDirectories(repo);
    Path dot = repo.resolve(".tvcs");
    Files.createDirectories(dot.resolve("refs/heads"));
    Files.createDirectories(dot.resolve("objects/commits"));
    Files.createDirectories(dot.resolve("objects/snapshots"));
    Files.writeString(dot.resolve("HEAD"), "ref: refs/heads/main\n", StandardCharsets.UTF_8);
    Files.writeString(dot.resolve("refs/heads/main"), "NONE\n", StandardCharsets.UTF_8);
    return "initialized empty repository in " + args.get(1);
  }

  private String cmdCommit(List<String> args) throws IOException {
    if (args.size() < 4) {
      throw new TvcsException("usage: tvcs commit <repo_dir> <message> <staging_dir>", -1, "");
    }
    Path root = resolveRepoRoot(Path.of(args.get(1)));
    String msg = args.get(2);
    Path staging = Path.of(args.get(3));

    String snap = TvcsFnv.snapshotId(staging);
    try (var stream = Files.list(staging)) {
      var names =
          stream
              .map(p -> p.getFileName().toString())
              .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".csv"))
              .sorted()
              .toList();
      if (names.isEmpty()) {
        throw new TvcsException("staging folder has no csvs", -1, "");
      }
      Path snapDir = root.resolve(".tvcs/objects/snapshots").resolve(snap);
      Files.createDirectories(snapDir);
      for (String name : names) {
        Files.copy(staging.resolve(name), snapDir.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    }

    String branch = readHeadBranch(root);
    String parent = readRefCommit(root, branch);
    long epoch = Instant.now().getEpochSecond();
    String body = String.format(Locale.ROOT, "parent %s\nsnapshot %s\ntime %d\nmessage %s\n", parent, snap, epoch, msg);
    if (body.length() > 8192) {
      throw new TvcsException("message is huge, shorten it", -1, "");
    }
    String commitId = TvcsFnv.fnv1aHex(body.getBytes(StandardCharsets.UTF_8));
    Path commitFile = root.resolve(".tvcs/objects/commits").resolve(commitId);
    Files.writeString(commitFile, body, StandardCharsets.UTF_8);
    writeRefCommit(root, branch, commitId);
    return commitId;
  }

  private String cmdLog(List<String> args) throws IOException {
    if (args.size() < 2) {
      throw new TvcsException("usage: tvcs log <repo_dir>", -1, "");
    }
    Path root = resolveRepoRoot(Path.of(args.get(1)));
    String branch = readHeadBranch(root);
    String cur = readRefCommit(root, branch);
    StringBuilder out = new StringBuilder();
    while (!"NONE".equals(cur)) {
      Path commitPath = root.resolve(".tvcs/objects/commits").resolve(cur);
      if (!Files.isRegularFile(commitPath)) {
        throw new TvcsException("commit file vanished (?)", -1, "");
      }
      String data = Files.readString(commitPath, StandardCharsets.UTF_8);
      out.append("commit ").append(cur).append('\n');
      for (String line : data.split("\\R")) {
        out.append("  ").append(line).append('\n');
      }
      out.append('\n');
      cur = parseParentLine(data);
    }
    return out.toString().trim();
  }

  private String cmdHead(List<String> args) throws IOException {
    if (args.size() < 2) {
      throw new TvcsException("usage: tvcs head <repo_dir>", -1, "");
    }
    Path root = resolveRepoRoot(Path.of(args.get(1)));
    String branch = readHeadBranch(root);
    String cur = readRefCommit(root, branch);
    return branch + " " + cur;
  }

  private static Path resolveRepoRoot(Path start) throws IOException {
    Path cur = start.toAbsolutePath().normalize();
    while (cur != null) {
      if (Files.isDirectory(cur.resolve(".tvcs"))) {
        return cur;
      }
      cur = cur.getParent();
    }
    throw new TvcsException("no .tvcs here", -1, "");
  }

  private static String readHeadBranch(Path root) throws IOException {
    Path head = root.resolve(".tvcs/HEAD");
    String data = Files.readString(head, StandardCharsets.UTF_8).trim();
    if (data.startsWith("ref:")) {
      String r = data.substring(4).trim();
      String prefix = "refs/heads/";
      if (r.startsWith(prefix)) {
        return r.substring(prefix.length()).trim();
      }
    }
    throw new TvcsException("HEAD looks wrong", -1, "");
  }

  private static String readRefCommit(Path root, String branch) throws IOException {
    Path p = root.resolve(".tvcs/refs/heads").resolve(branch);
    if (!Files.isRegularFile(p)) {
      return "NONE";
    }
    String v = Files.readString(p, StandardCharsets.UTF_8).trim();
    if (v.isEmpty() || "NONE".equals(v)) {
      return "NONE";
    }
    if (v.length() > HASH_LEN) {
      v = v.substring(0, HASH_LEN);
    }
    return v;
  }

  private static void writeRefCommit(Path root, String branch, String commit) throws IOException {
    Path dir = root.resolve(".tvcs/refs/heads");
    Files.createDirectories(dir);
    Files.writeString(dir.resolve(branch), commit + "\n", StandardCharsets.UTF_8);
  }

  private static String parseParentLine(String commitFile) {
    int idx = commitFile.indexOf("parent ");
    if (idx < 0) {
      return "NONE";
    }
    int start = idx + 7;
    int end = start;
    while (end < commitFile.length()) {
      char c = commitFile.charAt(end);
      if (c == '\n' || c == '\r') {
        break;
      }
      end++;
    }
    String p = commitFile.substring(start, end);
    if (p.length() > HASH_LEN) {
      p = p.substring(0, HASH_LEN);
    }
    return p.isEmpty() ? "NONE" : p;
  }
}
