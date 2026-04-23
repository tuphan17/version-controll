package com.tabularhub.engine;

public class TvcsException extends RuntimeException {

  private final int exitCode;
  private final String stderr;

  public TvcsException(String message, int exitCode, String stderr) {
    super(message);
    this.exitCode = exitCode;
    this.stderr = stderr;
  }

  public int getExitCode() {
    return exitCode;
  }

  public String getStderr() {
    return stderr;
  }
}
