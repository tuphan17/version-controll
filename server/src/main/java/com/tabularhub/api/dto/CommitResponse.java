package com.tabularhub.api.dto;

public class CommitResponse {

  private String commitId;

  public CommitResponse() {}

  public CommitResponse(String commitId) {
    this.commitId = commitId;
  }

  public String getCommitId() {
    return commitId;
  }

  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }
}
