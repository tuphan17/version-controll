#include "tvcs_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#ifdef _WIN32
#include <direct.h>
#include <io.h>
#define TVCS_ACCESS(p) _access((p), 0)
#else
#include <unistd.h>
#define TVCS_ACCESS(p) access((p), 0)
#endif

static void die(const char *msg) {
  fprintf(stderr, "%s\n", msg);
  exit(1);
}

static int read_head_branch(const char *repo, char branch[256]) {
  char p[TVCS_PATH_MAX];
  snprintf(p, sizeof p, "%s/.tvcs/HEAD", repo);
  char *data = NULL;
  size_t len = 0;
  if (tvcs_read_file(p, &data, &len) != 0)
    return -1;
  if (len >= 5 && strncmp(data, "ref: ", 5) == 0) {
    const char *r = data + 5;
    while (*r == ' ' || *r == '\t')
      r++;
    const char *base = "refs/heads/";
    if (strncmp(r, base, strlen(base)) == 0) {
      snprintf(branch, 256, "%s", r + strlen(base));
      char *nl = strchr(branch, '\r');
      if (nl)
        *nl = '\0';
      nl = strchr(branch, '\n');
      if (nl)
        *nl = '\0';
      free(data);
      return 0;
    }
  }
  free(data);
  return -1;
}

static int read_ref_commit(const char *repo, const char *branch, char commit[TVCS_HEX_HASH_LEN + 1]) {
  char p[TVCS_PATH_MAX];
  snprintf(p, sizeof p, "%s/.tvcs/refs/heads/%s", repo, branch);
  char *data = NULL;
  size_t len = 0;
  if (tvcs_read_file(p, &data, &len) != 0) {
    snprintf(commit, TVCS_HEX_HASH_LEN + 1, "NONE");
    return 0;
  }
  while (len && (data[len - 1] == '\n' || data[len - 1] == '\r'))
    data[--len] = '\0';
  if (len == 0 || strcmp(data, "NONE") == 0) {
    free(data);
    snprintf(commit, TVCS_HEX_HASH_LEN + 1, "NONE");
    return 0;
  }
  snprintf(commit, TVCS_HEX_HASH_LEN + 1, "%s", data);
  free(data);
  return 0;
}

static void write_ref_commit(const char *repo, const char *branch, const char *commit) {
  char p[TVCS_PATH_MAX];
  snprintf(p, sizeof p, "%s/.tvcs/refs/heads", repo);
  tvcs_mkpath(p);
  snprintf(p, sizeof p, "%s/.tvcs/refs/heads/%s", repo, branch);
  char line[64];
  int n = snprintf(line, sizeof line, "%s\n", commit);
  if (n <= 0 || (size_t)n >= sizeof line)
    die("ref name too long");
  tvcs_write_file(p, line, (size_t)n);
}

static void commit_hash_from_body(const char *body, size_t blen, char out[TVCS_HEX_HASH_LEN + 1]) {
  tvcs_fnv1a_hex(body, blen, out);
}

static int cmd_init(int argc, char **argv) {
  if (argc < 3)
    die("usage: tvcs init <repo_dir>");
  const char *repo = argv[2];
  char root[TVCS_PATH_MAX];
  snprintf(root, sizeof root, "%s", repo);
  if (tvcs_mkpath(root) != 0)
    die("mkpath repo failed");
  char dot[TVCS_PATH_MAX];
  snprintf(dot, sizeof dot, "%s/.tvcs", repo);
  char sub[TVCS_PATH_MAX];
  snprintf(sub, sizeof sub, "%s/refs/heads", dot);
  if (tvcs_mkpath(sub) != 0)
    die("mkpath failed");
  snprintf(sub, sizeof sub, "%s/objects/commits", dot);
  if (tvcs_mkpath(sub) != 0)
    die("mkpath failed");
  snprintf(sub, sizeof sub, "%s/objects/snapshots", dot);
  if (tvcs_mkpath(sub) != 0)
    die("mkpath failed");
  char hp[TVCS_PATH_MAX];
  snprintf(hp, sizeof hp, "%s/HEAD", dot);
  tvcs_write_file(hp, "ref: refs/heads/main\n", strlen("ref: refs/heads/main\n"));
  snprintf(hp, sizeof hp, "%s/refs/heads/main", dot);
  tvcs_write_file(hp, "NONE\n", strlen("NONE\n"));
  printf("initialized empty repository in %s\n", repo);
  return 0;
}

static int cmd_commit(int argc, char **argv) {
  if (argc < 5)
    die("usage: tvcs commit <repo_dir> <message> <staging_dir>");
  const char *repo = argv[2];
  const char *msg = argv[3];
  const char *staging = argv[4];
  char root[TVCS_PATH_MAX];
  if (tvcs_repo_resolve(repo, root) != 0)
    die("not a repository");
  char snap[TVCS_HEX_HASH_LEN + 1];
  if (tvcs_snapshot_id(staging, snap) != 0)
    die("snapshot failed (staging dir?)");
  char **names = NULL;
  int n = 0;
  if (tvcs_list_csv(staging, &names, &n) != 0)
    die("list csv failed");
  if (n == 0) {
    tvcs_free_lines(names, n);
    die("no .csv files in staging");
  }
  char snapdir[TVCS_PATH_MAX];
  snprintf(snapdir, sizeof snapdir, "%s/.tvcs/objects/snapshots/%s", root, snap);
  if (tvcs_mkpath(snapdir) != 0)
    die("mkpath snapshot");
  for (int i = 0; i < n; i++) {
    char src[TVCS_PATH_MAX];
    char dst[TVCS_PATH_MAX];
    snprintf(src, sizeof src, "%s/%s", staging, names[i]);
    snprintf(dst, sizeof dst, "%s/%s", snapdir, names[i]);
#ifdef _WIN32
    for (char *p = src; *p; p++) {
      if (*p == '/')
        *p = '\\';
    }
    for (char *p = dst; *p; p++) {
      if (*p == '/')
        *p = '\\';
    }
#endif
    if (tvcs_copy_file(src, dst) != 0)
      die("copy failed");
  }
  tvcs_free_lines(names, n);

  char branch[256];
  if (read_head_branch(root, branch) != 0)
    die("read HEAD");
  char parent[TVCS_HEX_HASH_LEN + 1];
  read_ref_commit(root, branch, parent);

  unsigned long long t = (unsigned long long)time(NULL);
  char body[8192];
  int bl = snprintf(body, sizeof body, "parent %s\nsnapshot %s\ntime %llu\nmessage %s\n", parent, snap,
                    (unsigned long long)t, msg);
  if (bl <= 0 || (size_t)bl >= sizeof body)
    die("commit message too long");
  char ch[TVCS_HEX_HASH_LEN + 1];
  commit_hash_from_body(body, (size_t)bl, ch);

  char cp[TVCS_PATH_MAX];
  snprintf(cp, sizeof cp, "%s/.tvcs/objects/commits/%s", root, ch);
  if (tvcs_write_file(cp, body, (size_t)bl) != 0)
    die("write commit");
  write_ref_commit(root, branch, ch);
  printf("%s\n", ch);
  return 0;
}

static int cmd_log(int argc, char **argv) {
  if (argc < 3)
    die("usage: tvcs log <repo_dir>");
  const char *repo = argv[2];
  char root[TVCS_PATH_MAX];
  if (tvcs_repo_resolve(repo, root) != 0)
    die("not a repository");
  char branch[256];
  if (read_head_branch(root, branch) != 0)
    die("read HEAD");
  char cur[TVCS_HEX_HASH_LEN + 1];
  read_ref_commit(root, branch, cur);
  while (strcmp(cur, "NONE") != 0) {
    char cp[TVCS_PATH_MAX];
    snprintf(cp, sizeof cp, "%s/.tvcs/objects/commits/%s", root, cur);
    char *data = NULL;
    size_t len = 0;
    if (tvcs_read_file(cp, &data, &len) != 0)
      die("missing commit object");
    printf("commit %s\n", cur);
    const char *r = data;
    const char *end = data + len;
    while (r < end) {
      const char *e = r;
      while (e < end && *e != '\n' && *e != '\r')
        e++;
      printf("  %.*s\n", (int)(e - r), r);
      r = e;
      while (r < end && (*r == '\n' || *r == '\r'))
        r++;
    }
    printf("\n");
    char next[TVCS_HEX_HASH_LEN + 1];
    snprintf(next, sizeof next, "NONE");
    const char *p = strstr(data, "parent ");
    if (p) {
      p += 7;
      const char *eol = strpbrk(p, "\r\n");
      size_t plen = eol ? (size_t)(eol - p) : strlen(p);
      if (plen > TVCS_HEX_HASH_LEN)
        plen = TVCS_HEX_HASH_LEN;
      memcpy(next, p, plen);
      next[plen] = '\0';
    }
    free(data);
    snprintf(cur, sizeof cur, "%s", next);
  }
  return 0;
}

static int cmd_branch(int argc, char **argv) {
  if (argc < 5 || strcmp(argv[3], "create") != 0)
    die("usage: tvcs branch <repo_dir> create <name>");
  const char *repo = argv[2];
  const char *name = argv[4];
  char root[TVCS_PATH_MAX];
  if (tvcs_repo_resolve(repo, root) != 0)
    die("not a repository");
  char branch[256];
  if (read_head_branch(root, branch) != 0)
    die("read HEAD");
  char cur[TVCS_HEX_HASH_LEN + 1];
  read_ref_commit(root, branch, cur);
  char p[TVCS_PATH_MAX];
  snprintf(p, sizeof p, "%s/.tvcs/refs/heads/%s", root, name);
  char line[64];
  int ll = snprintf(line, sizeof line, "%s\n", cur);
  if (ll <= 0 || (size_t)ll >= sizeof line)
    die("ref value too long");
  tvcs_write_file(p, line, (size_t)ll);
  printf("branch %s at %s\n", name, cur);
  return 0;
}

static int cmd_checkout(int argc, char **argv) {
  if (argc < 5)
    die("usage: tvcs checkout <repo_dir> <commit_or_branch> <out_dir>");
  const char *repo = argv[2];
  const char *rev = argv[3];
  const char *outd = argv[4];
  char root[TVCS_PATH_MAX];
  if (tvcs_repo_resolve(repo, root) != 0)
    die("not a repository");
  char commit[TVCS_HEX_HASH_LEN + 1];
  snprintf(commit, sizeof commit, "%s", rev);
  char bp[TVCS_PATH_MAX];
  snprintf(bp, sizeof bp, "%s/.tvcs/refs/heads/%s", root, rev);
  if (TVCS_ACCESS(bp) == 0) {
    read_ref_commit(root, rev, commit);
  }
  if (strcmp(commit, "NONE") == 0)
    die("cannot checkout empty branch");
  char cp[TVCS_PATH_MAX];
  snprintf(cp, sizeof cp, "%s/.tvcs/objects/commits/%s", root, commit);
  char *data = NULL;
  size_t len = 0;
  if (tvcs_read_file(cp, &data, &len) != 0)
    die("bad rev");
  char snap[TVCS_HEX_HASH_LEN + 1];
  snap[0] = '\0';
  const char *sl = strstr(data, "snapshot ");
  if (sl) {
    sl += 9;
    const char *eol = strpbrk(sl, "\r\n");
    size_t slen = eol ? (size_t)(eol - sl) : strlen(sl);
    if (slen > TVCS_HEX_HASH_LEN)
      slen = TVCS_HEX_HASH_LEN;
    memcpy(snap, sl, slen);
    snap[slen] = '\0';
  }
  free(data);
  if (!snap[0])
    die("commit missing snapshot");
  char snapdir[TVCS_PATH_MAX];
  snprintf(snapdir, sizeof snapdir, "%s/.tvcs/objects/snapshots/%s", root, snap);
  char outbuf[TVCS_PATH_MAX];
  snprintf(outbuf, sizeof outbuf, "%s", outd);
  if (tvcs_mkpath(outbuf) != 0)
    die("mkpath out");
  char **names = NULL;
  int n = 0;
  if (tvcs_list_csv(snapdir, &names, &n) != 0)
    die("list snapshot");
  for (int i = 0; i < n; i++) {
    char src[TVCS_PATH_MAX];
    char dst[TVCS_PATH_MAX];
    snprintf(src, sizeof src, "%s/%s", snapdir, names[i]);
    snprintf(dst, sizeof dst, "%s/%s", outd, names[i]);
#ifdef _WIN32
    for (char *p = src; *p; p++) {
      if (*p == '/')
        *p = '\\';
    }
    for (char *p = dst; *p; p++) {
      if (*p == '/')
        *p = '\\';
    }
#endif
    if (tvcs_copy_file(src, dst) != 0)
      die("checkout copy failed");
  }
  tvcs_free_lines(names, n);
  printf("checked out %s to %s\n", commit, outd);
  return 0;
}

static int cmd_head(int argc, char **argv) {
  if (argc < 3)
    die("usage: tvcs head <repo_dir>");
  const char *repo = argv[2];
  char root[TVCS_PATH_MAX];
  if (tvcs_repo_resolve(repo, root) != 0)
    die("not a repository");
  char branch[256];
  if (read_head_branch(root, branch) != 0)
    die("read HEAD");
  char cur[TVCS_HEX_HASH_LEN + 1];
  read_ref_commit(root, branch, cur);
  printf("%s %s\n", branch, cur);
  return 0;
}

int main(int argc, char **argv) {
  if (argc < 2)
    die("tvcs: init | commit | log | branch | checkout | head");
  if (strcmp(argv[1], "init") == 0)
    return cmd_init(argc, argv);
  if (strcmp(argv[1], "commit") == 0)
    return cmd_commit(argc, argv);
  if (strcmp(argv[1], "log") == 0)
    return cmd_log(argc, argv);
  if (strcmp(argv[1], "branch") == 0)
    return cmd_branch(argc, argv);
  if (strcmp(argv[1], "checkout") == 0)
    return cmd_checkout(argc, argv);
  if (strcmp(argv[1], "head") == 0)
    return cmd_head(argc, argv);
  die("unknown command");
  return 1;
}
