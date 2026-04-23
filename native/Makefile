# Optional: build without CMake — requires a C99 compiler (gcc or clang).
CFLAGS ?= -std=c99 -Wall -Wextra -pedantic -Iinclude
SRC = src/tvcs.c src/fsutil.c src/hash.c src/repo.c

tvcs: $(SRC)
	$(CC) $(CFLAGS) -o tvcs $(SRC)

clean:
	rm -f tvcs

.PHONY: clean
