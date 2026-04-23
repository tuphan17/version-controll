#ifndef TVCS_INTERNAL_H
#define TVCS_INTERNAL_H

#include <stddef.h>

#define TVCS_HEX_HASH_LEN 16
#define TVCS_PATH_MAX 4096

int tvcs_mkpath(char *path);
int tvcs_write_file(const char *path, const void *data, size_t len);
int tvcs_read_file(const char *path, char **out, size_t *out_len);
int tvcs_copy_file(const char *src, const char *dst);

/** List *.csv in directory; caller frees *names with free_lines. Returns count or -1. */
int tvcs_list_csv(const char *dir, char ***names, int *count);
void tvcs_free_lines(char **names, int count);

void tvcs_fnv1a_hex(const void *data, size_t len, char out[TVCS_HEX_HASH_LEN + 1]);
void tvcs_fnv1a_hex_file(const char *path, char out[TVCS_HEX_HASH_LEN + 1]);

/** Hash of snapshot: sorted table names + each file hash, deterministic. */
int tvcs_snapshot_id(const char *staging_dir, char out[TVCS_HEX_HASH_LEN + 1]);

int tvcs_repo_resolve(const char *start, char root[TVCS_PATH_MAX]);

#endif
