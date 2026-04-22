package com.iris.back.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BusinessExceptionTests {

  @Test
  void compiledClassExposesCodeGetter() {
    Method getter = Arrays.stream(BusinessException.class.getMethods())
        .filter(method -> method.getName().equals("getCode"))
        .findFirst()
        .orElse(null);

    assertThat(getter).isNotNull();
  }
}
