package com.iris.back.business.standard.controller;

import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.request.StandardUpsertRequest;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/standards")
public class StandardController {

  private final StandardService standardService;

  public StandardController(StandardService standardService) {
    this.standardService = standardService;
  }

  @GetMapping
  public ApiResponse<List<StandardDto>> list() {
    return ApiResponse.success(standardService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<StandardDto> get(@PathVariable String id) {
    return ApiResponse.success(standardService.get(id));
  }

  @PostMapping
  public ApiResponse<StandardDto> create(@Valid @RequestBody StandardUpsertRequest request) {
    return ApiResponse.success("standard created", standardService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<StandardDto> update(
      @PathVariable String id,
      @Valid @RequestBody StandardUpsertRequest request
  ) {
    return ApiResponse.success("standard updated", standardService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    standardService.delete(id);
    return ApiResponse.success("standard deleted", null);
  }
}
