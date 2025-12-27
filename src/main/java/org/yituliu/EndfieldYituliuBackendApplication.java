package org.yituliu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("org.yituliu.mapper")
public class EndfieldYituliuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EndfieldYituliuBackendApplication.class, args);
    }

}
