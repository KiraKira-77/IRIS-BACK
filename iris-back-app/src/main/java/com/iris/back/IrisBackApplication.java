package com.iris.back;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
    "com.iris.back.system.mapper",
    "com.iris.back.business.standard.mapper"
})
public class IrisBackApplication {

  public static void main(String[] args) {
    SpringApplication.run(IrisBackApplication.class, args);
  }
}
