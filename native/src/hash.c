#include "tvcs_internal.h"
#include <stdio.h>

static void fnv1a64_update(unsigned long long *h, const unsigned char *p, size_t n) {
  unsigned long long x = *h;
  for (size_t i = 0; i < n; i++) {
    x ^= (unsigned long long)p[i];
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

void tvcs_fnv1a_hex(const void *data, size_t len, char out[TVCS_HEX_HASH_LEN + 1]) {
  unsigned long long h = 14695981039346656037ULL;
  fnv1a64_update(&h, (const unsigned char *)data, len);
  hex16(h, out);
}

void tvcs_fnv1a_hex_file(const char *path, char out[TVCS_HEX_HASH_LEN + 1]) {
  FILE *f = fopen(path, "rb");
  if (!f) {
    out[0] = '\0';
    return;
  }
  unsigned long long h = 14695981039346656037ULL;
  unsigned char buf[8192];
  size_t n;
  while ((n = fread(buf, 1, sizeof buf, f)) > 0) {
    fnv1a64_update(&h, buf, n);
  }
  fclose(f);
  hex16(h, out);
}
