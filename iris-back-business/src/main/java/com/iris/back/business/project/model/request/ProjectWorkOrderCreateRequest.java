package com.iris.back.business.project.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProjectWorkOrderCreateRequest(
    String title,
    String description,
    @Valid @NotEmpty List<HandlerRequest> handlers
) {

  public record HandlerRequest(
      @NotBlank String handlerId,
      @NotBlank String handlerEmployeeNo,
      @NotBlank String handlerName
  ) {
  }
}
