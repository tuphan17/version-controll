#if !defined(_WIN32) && !defined(_POSIX_C_SOURCE)
#define _POSIX_C_SOURCE 200809L
#endif

#include "tvcs_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <direct.h>
#include <io.h>
#define TVCS_ACCESS(p) _access((p), 0)
#else
#include <unistd.h>
#define TVCS_ACCESS(p) access((p), 0)
#endif

#ifdef _WIN32
#include <windows.h>
#endif

#ifndef _WIN32
static int cmp_str(const void *a, const void *b) {
  return strcmp(*(const char *const *)a, *(const char *const *)b);
}
#endif

int tvcs_repo_resolve(const char *start, char root[TVCS_PATH_MAX]) {
#ifdef _WIN32
  char buf[TVCS_PATH_MAX];
  if (!GetFullPathNameA(start, sizeof buf, buf, NULL))
    return -1;
#else
  char buf[TVCS_PATH_MAX];
  if (!realpath(start, buf)) {
    /* realpath requires existing path; walk manually */
    snprintf(buf, sizeof buf, "%s", start);
  }
#endif
  char cur[TVCS_PATH_MAX];
  snprintf(cur, sizeof cur, "%s", buf);
  for (;;) {
    char probe[TVCS_PATH_MAX];
    snprintf(probe, sizeof probe, "%s/.tvcs", cur);
    if (TVCS_ACCESS(probe) == 0) {
      snprintf(root, TVCS_PATH_MAX, "%s", cur);
      return 0;
    }
    char *slash = strrchr(cur, '/');
#ifdef _WIN32
    char *bs = strrchr(cur, '\\');
    if (!slash || (bs && bs > slash))
      slash = bs;
#endif
    if (!slash || slash == cur)
      return -1;
    *slash = '\0';
  }
}

static void fnv_mix(unsigned long long *h, const char *s) {
  unsigned long long x = *h;
  const unsigned char *p = (const unsigned char *)s;
  while (*p) {
    x ^= (unsigned long long)*p++;
    x *= 1099511628211ULL;
  }
  *h = x;
}

static void hex16(unsigned long long v, char out[TVCS_HEX_HASH_LEN + 1]) {
  static const char *xd = "0123456789abcdef";
  for (int i = 0; i < 16; i++) {
    int shift = (15 - i) * 4;
    out[i] = xd[(int)((v >> shift) & 0xFULL)];
  }
  out[16] = '\0';
}

int tvcs_snapshot_id(const char *staging_dir, char out[TVCS_HEX_HASH_LEN + 1]) {
  char **names = NULL;
  int n = 0;
  if (tvcs_list_csv(staging_dir, &names, &n) != 0)
    return -1;
#ifdef _WIN32
  /* Deterministic order on Windows (list order not sorted). */
  for (int i = 0; i < n - 1; i++) {
    for (int j = i + 1; j < n; j++) {
      if (strcmp(names[i], names[j]) > 0) {
        char *t = names[i];
        names[i] = names[j];
        names[j] = t;
      }
    }
  }
#else
  qsort(names, (size_t)n, sizeof *names, cmp_str);
#endif
  unsigned long long h = 14695981039346656037ULL;
  for (int i = 0; i < n; i++) {
    char path[TVCS_PATH_MAX];
    snprintf(path, sizeof path, "%s/%s", staging_dir, names[i]);
#ifdef _WIN32
    for (char *p = path; *p; p++) {
      if (*p == '/')
        *p = '\\';
    }
#endif
    char fh[TVCS_HEX_HASH_LEN + 1];
    tvcs_fnv1a_hex_file(path, fh);
    if (!fh[0]) {
      tvcs_free_lines(names, n);
      return -1;
    }
    fnv_mix(&h, names[i]);
    fnv_mix(&h, ":");
    fnv_mix(&h, fh);
    fnv_mix(&h, "|");
  }
  hex16(h, out);
  tvcs_free_lines(names, n);
  return 0;
}
