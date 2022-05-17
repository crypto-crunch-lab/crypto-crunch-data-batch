package com.crypto.crunch.batch.defi;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

@ActiveProfiles("local")
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DefiPlatformUpdateServiceImplTest {
    @Autowired
    DefiPlatformUpdateServiceImpl defiPlatformUpdateService;

    @Test
    void updatePlatform() throws IOException {
        defiPlatformUpdateService.updatePlatform();
    }
}
