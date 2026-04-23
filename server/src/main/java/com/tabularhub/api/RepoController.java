package com.tabularhub.api;

import com.tabularhub.api.dto.CommitRequest;
import com.tabularhub.api.dto.CommitResponse;
import com.tabularhub.api.dto.CreateRepoRequest;
import com.tabularhub.api.dto.RepoResponse;
import com.tabularhub.service.RepoService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repos")
public class RepoController {

  private final RepoService repoService;

  public RepoController(RepoService repoService) {
    this.repoService = repoService;
  }

  @GetMapping
  public List<RepoResponse> list() {
    return repoService.list();
  }

  @GetMapping("/{id}")
  public RepoResponse get(@PathVariable String id) {
    return repoService.get(id);
  }

  @PostMapping
  public RepoResponse create(@RequestBody CreateRepoRequest body) {
    return repoService.create(body);
  }

  @PostMapping("/{id}/commits")
  public CommitResponse commit(@PathVariable String id, @RequestBody CommitRequest body) {
    return repoService.commit(id, body);
  }

  @GetMapping(value = "/{id}/log", produces = MediaType.TEXT_PLAIN_VALUE)
  public String log(@PathVariable String id) {
    return repoService.log(id);
  }

  @GetMapping(value = "/{id}/head", produces = MediaType.TEXT_PLAIN_VALUE)
  public String head(@PathVariable String id) {
    return repoService.head(id);
  }
}
