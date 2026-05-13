package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

class IrisBackApplicationConfigurationTests {

  @Test
  void mapperScanIncludesAiModelMapperPackage() {
    MapperScan mapperScan = IrisBackApplication.class.getAnnotation(MapperScan.class);

    assertThat(Arrays.asList(mapperScan.value()))
        .contains("com.iris.back.business.ai.mapper");
  }
}
