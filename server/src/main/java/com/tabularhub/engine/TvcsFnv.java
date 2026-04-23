package com.tabularhub.engine;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** FNV-1a 64-bit and 16-hex formatting matching the native `tvcs` implementation. */
public final class TvcsFnv {

  private static final BigInteger MASK64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
  private static final BigInteger PRIME = BigInteger.valueOf(1099511628211L);
  private static final BigInteger OFFSET = new BigInteger("14695981039346656037");

  private TvcsFnv() {}

  public static BigInteger fnv1a64(byte[] data) {
    BigInteger h = OFFSET;
    for (byte b : data) {
      h = h.xor(BigInteger.valueOf(b & 0xffL));
      h = h.multiply(PRIME).and(MASK64);
    }
    return h;
  }

  public static String hex16(BigInteger h) {
    BigInteger v = h.and(MASK64);
    StringBuilder sb = new StringBuilder(16);
    for (int i = 15; i >= 0; i--) {
      int nibble = v.shiftRight(i * 4).and(BigInteger.valueOf(15)).intValue();
      sb.append("0123456789abcdef".charAt(nibble));
    }
    return sb.toString();
  }

  public static String fnv1aHex(byte[] data) {
    return hex16(fnv1a64(data));
  }

  public static BigInteger mixUtf8(BigInteger h, String s) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      h = h.xor(BigInteger.valueOf(b & 0xffL));
      h = h.multiply(PRIME).and(MASK64);
    }
    return h;
  }

  public static String fnv1aHexFile(Path path) throws java.io.IOException {
    return fnv1aHex(Files.readAllBytes(path));
  }

  public static String snapshotId(Path stagingDir) throws java.io.IOException {
    try (var stream = Files.list(stagingDir)) {
      var names =
          stream
              .map(p -> p.getFileName().toString())
              .filter(n -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
              .sorted()
              .toList();
      if (names.isEmpty()) {
        throw new TvcsException("no .csv files in staging", -1, "");
      }
      BigInteger h = OFFSET;
      for (String name : names) {
        h = mixUtf8(h, name);
        h = mixUtf8(h, ":");
        h = mixUtf8(h, fnv1aHexFile(stagingDir.resolve(name)));
        h = mixUtf8(h, "|");
      }
      return hex16(h);
    }
  }
}
