#include "tvcs_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <direct.h>
#include <io.h>
#include <windows.h>
#define PATH_SEP '\\'
#else
#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>
#define PATH_SEP '/'
#endif

int tvcs_mkpath(char *path) {
  for (char *p = path + 1; *p; p++) {
    if (*p == '/' || *p == '\\') {
      char c = *p;
      *p = '\0';
#ifdef _WIN32
      if (_mkdir(path) != 0 && _access(path, 0) != 0) {
#else
      if (mkdir(path, 0755) != 0) {
#endif
        /* ignore if exists */
#ifdef _WIN32
        if (_access(path, 0) != 0) {
          *p = c;
          return -1;
        }
#else
        struct stat st;
        if (stat(path, &st) != 0) {
          *p = c;
          return -1;
        }
#endif
      }
      *p = c;
    }
  }
#ifdef _WIN32
  if (_mkdir(path) != 0 && _access(path, 0) != 0)
    return -1;
#else
  if (mkdir(path, 0755) != 0) {
    struct stat st;
    if (stat(path, &st) != 0)
      return -1;
  }
#endif
  return 0;
}

int tvcs_write_file(const char *path, const void *data, size_t len) {
  FILE *f = fopen(path, "wb");
  if (!f)
    return -1;
  if (len && fwrite(data, 1, len, f) != len) {
    fclose(f);
    return -1;
  }
  fclose(f);
  return 0;
}

int tvcs_read_file(const char *path, char **out, size_t *out_len) {
  FILE *f = fopen(path, "rb");
  if (!f)
    return -1;
  if (fseek(f, 0, SEEK_END) != 0) {
    fclose(f);
    return -1;
  }
  long sz = ftell(f);
  if (sz < 0) {
    fclose(f);
    return -1;
  }
  rewind(f);
  char *buf = (char *)malloc((size_t)sz + 1);
  if (!buf) {
    fclose(f);
    return -1;
  }
  if (sz && fread(buf, 1, (size_t)sz, f) != (size_t)sz) {
    free(buf);
    fclose(f);
    return -1;
  }
  buf[sz] = '\0';
  fclose(f);
  *out = buf;
  *out_len = (size_t)sz;
  return 0;
}

int tvcs_copy_file(const char *src, const char *dst) {
  FILE *in = fopen(src, "rb");
  if (!in)
    return -1;
  FILE *out = fopen(dst, "wb");
  if (!out) {
    fclose(in);
    return -1;
  }
  unsigned char buf[8192];
  size_t n;
  while ((n = fread(buf, 1, sizeof buf, in)) > 0) {
    if (fwrite(buf, 1, n, out) != n) {
      fclose(in);
      fclose(out);
      return -1;
    }
  }
  fclose(in);
  fclose(out);
  return 0;
}

static int ends_with_csv(const char *name) {
  size_t n = strlen(name);
  return n > 4 && strcmp(name + n - 4, ".csv") == 0;
}

#ifdef _WIN32
int tvcs_list_csv(const char *dir, char ***names, int *count) {
  char pattern[TVCS_PATH_MAX];
  snprintf(pattern, sizeof pattern, "%s\\*.csv", dir);
  WIN32_FIND_DATAA fd;
  HANDLE h = FindFirstFileA(pattern, &fd);
  if (h == INVALID_HANDLE_VALUE) {
    *names = NULL;
    *count = 0;
    return 0;
  }
  int cap = 8;
  int n = 0;
  char **arr = (char **)malloc((size_t)cap * sizeof *arr);
  if (!arr) {
    FindClose(h);
    return -1;
  }
  do {
    if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) && ends_with_csv(fd.cFileName)) {
      if (n >= cap) {
        cap *= 2;
        char **na = (char **)realloc(arr, (size_t)cap * sizeof *na);
        if (!na) {
          for (int i = 0; i < n; i++)
            free(arr[i]);
          free(arr);
          FindClose(h);
          return -1;
        }
        arr = na;
      }
      arr[n] = strdup(fd.cFileName);
      if (!arr[n]) {
        for (int i = 0; i < n; i++)
          free(arr[i]);
        free(arr);
        FindClose(h);
        return -1;
      }
      n++;
    }
  } while (FindNextFileA(h, &fd));
  FindClose(h);
  *names = arr;
  *count = n;
  return 0;
}
#else
static int cmp_str(const void *a, const void *b) {
  return strcmp(*(const char *const *)a, *(const char *const *)b);
}

int tvcs_list_csv(const char *dir, char ***names, int *count) {
  DIR *d = opendir(dir);
  if (!d)
    return -1;
  int cap = 8;
  int n = 0;
  char **arr = (char **)malloc((size_t)cap * sizeof *arr);
  if (!arr) {
    closedir(d);
    return -1;
  }
  struct dirent *e;
  while ((e = readdir(d)) != NULL) {
    if (e->d_type != DT_REG && e->d_type != DT_UNKNOWN)
      continue;
    if (!ends_with_csv(e->d_name))
      continue;
    if (n >= cap) {
      cap *= 2;
      char **na = (char **)realloc(arr, (size_t)cap * sizeof *na);
      if (!na) {
        for (int i = 0; i < n; i++)
          free(arr[i]);
        free(arr);
        closedir(d);
        return -1;
      }
      arr = na;
    }
    arr[n] = strdup(e->d_name);
    if (!arr[n]) {
      for (int i = 0; i < n; i++)
        free(arr[i]);
      free(arr);
      closedir(d);
      return -1;
    }
    n++;
  }
  closedir(d);
  qsort(arr, (size_t)n, sizeof *arr, cmp_str);
  *names = arr;
  *count = n;
  return 0;
}
#endif

void tvcs_free_lines(char **names, int count) {
  for (int i = 0; i < count; i++)
    free(names[i]);
  free(names);
}
