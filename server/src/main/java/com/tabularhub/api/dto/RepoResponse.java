package com.tabularhub.api.dto;

import java.time.Instant;

public class RepoResponse {

  private String id;
  private String name;
  private String slug;
  private String filesystemPath;
  private Instant createdAt;

  public RepoResponse() {}

  public RepoResponse(String id, String name, String slug, String filesystemPath, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.filesystemPath = filesystemPath;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getFilesystemPath() {
    return filesystemPath;
  }

  public void setFilesystemPath(String filesystemPath) {
    this.filesystemPath = filesystemPath;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
