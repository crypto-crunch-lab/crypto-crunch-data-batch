package com.crypto.crunch.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

@Slf4j
@EnableBatchProcessing
@EnableFeignClients
@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class CrpytoCrunchDataBatchApplication {
    public static void main(String[] args) {

        String jobNames = System.getProperty("spring.batch.job.names");
        if (!StringUtils.hasText(jobNames)) {
            log.error("Jobname을 지정해주세요. (-Dspring.batch.job.names=[JOBNAME])");
            return;
        }

        ConfigurableApplicationContext run = SpringApplication.run(CrpytoCrunchDataBatchApplication.class, args);
        run.close();
    }
}
