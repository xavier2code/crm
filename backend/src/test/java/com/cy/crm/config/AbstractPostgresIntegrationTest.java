package com.cy.crm.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {
    // Local PostgreSQL used for integration tests.
    // Configure via backend/src/test/resources/application-test.yml
}
