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
public class DefiSyncBatch {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public DefiSyncService defiSyncService;

    @Bean
    public Job rCaptchaBatchJob(Step rCaptchaBatchStep) {
        return jobBuilderFactory.get("defiSyncBatchJob")
                .flow(rCaptchaBatchStep)
                .end().build();
    }

    @Bean
    public Step defiSyncBatchStep(DefiSyncService defiSyncService) {
        return stepBuilderFactory.get("defiSyncBatchStep")
                .tasklet((contribution, chunkContext) -> {
                    try {
                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        String param = (String) jobParameters.getOrDefault("param", "test");
                        log.info("jobParameters : {}", jobParameters);

                        defiSyncService.sync();

                    } catch (Exception e) {
                        e.printStackTrace();
                        // send error message
                        System.exit(-1);
                    }

                    return RepeatStatus.FINISHED;
                }).build();
    }
}
