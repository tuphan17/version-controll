package com.tabularhub.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommitRequest {

  private String message;
  private Map<String, String> tables = new LinkedHashMap<>();

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Map<String, String> getTables() {
    return tables;
  }

  public void setTables(Map<String, String> tables) {
    this.tables = tables;
  }
}
