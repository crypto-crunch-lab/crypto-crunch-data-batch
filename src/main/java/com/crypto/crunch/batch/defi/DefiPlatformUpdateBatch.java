package com.crypto.crunch.batch.defi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@EnableBatchProcessing
@Configuration
@Slf4j
public class DefiPlatformUpdateBatch {
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public DefiPlatformUpdateServiceImpl defiPlatformUpdateService;

    @Bean
    public Job defiPlatformUpdateBatchJob(Step defiPlatformUpdateBatchStep) {
        return jobBuilderFactory.get("defiPlatformUpdateBatchJob")
                .flow(defiPlatformUpdateBatchStep)
                .end().build();
    }

    @Bean
    public Step defiPlatformUpdateBatchStep(DefiPlatformUpdateServiceImpl defiPlatformUpdateService) {
        return stepBuilderFactory.get("defiPlatformUpdateBatchStep")
                .tasklet((contribution, chunkContext) -> {
                    try {
                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        String param = (String) jobParameters.getOrDefault("param", "test");
                        log.info("jobParameters : {}", jobParameters);

                        defiPlatformUpdateService.updatePlatform();

                    } catch (Exception e) {
                        e.printStackTrace();
                        // send error message
                        System.exit(-1);
                    }

                    return RepeatStatus.FINISHED;
                }).build();
    }
}

