package com.crypto.crunch.batch.defi;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@ActiveProfiles("test")
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DefiSyncBatchTest {
    @Autowired
    Job defiSyncBatchJob;

    @Autowired
    JobLauncher jobLauncher;

    @Test
    void defiSyncBatchStep() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("param", "paramTest")
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(defiSyncBatchJob, jobParameters);

        Assert.isTrue(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED), "");
    }
}
