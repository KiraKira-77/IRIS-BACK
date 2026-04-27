package com.iris.back.business.standard.controller;

import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.request.StandardListQuery;
import com.iris.back.business.standard.model.request.StandardRollbackRequest;
import com.iris.back.business.standard.model.request.StandardUpgradeRequest;
import com.iris.back.business.standard.model.request.StandardUpsertRequest;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import com.iris.back.system.model.dto.FileAttachmentDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/standards")
public class StandardController {

  private final StandardService standardService;

  public StandardController(StandardService standardService) {
    this.standardService = standardService;
  }

  @GetMapping
  public ApiResponse<PageResponse<StandardDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(standardService.list(new StandardListQuery(
        keyword,
        category,
        status,
        page,
        pageSize
    )));
  }

  @GetMapping("/{id}")
  public ApiResponse<StandardDto> get(@PathVariable String id) {
    return ApiResponse.success(standardService.get(id));
  }

  @PostMapping
  public ApiResponse<StandardDto> create(@Valid @RequestBody StandardUpsertRequest request) {
    return ApiResponse.success("standard created", standardService.create(request));
  }

  @PostMapping("/{id}/upgrade")
  public ApiResponse<StandardDto> upgrade(
      @PathVariable String id,
      @Valid @RequestBody StandardUpgradeRequest request
  ) {
    return ApiResponse.success("standard upgraded", standardService.upgrade(id, request));
  }

  @PostMapping("/{id}/publish")
  public ApiResponse<StandardDto> publish(@PathVariable String id) {
    return ApiResponse.success("standard published", standardService.publish(id));
  }

  @PostMapping("/{id}/rollback")
  public ApiResponse<StandardDto> rollback(
      @PathVariable String id,
      @Valid @RequestBody StandardRollbackRequest request
  ) {
    return ApiResponse.success("standard rollback draft created", standardService.rollback(id, request));
  }

  @GetMapping("/{id}/versions")
  public ApiResponse<List<StandardDto>> versions(@PathVariable String id) {
    return ApiResponse.success(standardService.versions(id));
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

  @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<FileAttachmentDto> uploadAttachment(
      @PathVariable String id,
      @RequestPart("file") MultipartFile file
  ) {
    return ApiResponse.success("standard attachment uploaded", standardService.uploadAttachment(id, file));
  }

  @DeleteMapping("/{id}/attachments/{fileId}")
  public ApiResponse<Void> deleteAttachment(@PathVariable String id, @PathVariable String fileId) {
    standardService.deleteAttachment(id, fileId);
    return ApiResponse.success("standard attachment deleted", null);
  }
}
