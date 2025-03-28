package com.aquiva.autotests.rc.internal.reporting;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit Extension class that provides additional logging for test execution context.
 * Useful to monitor test execution in real-time: locally, in CI/CD, or wherever else
 * test logging is present.
 */
public class JUnitLoggerExtension implements
        BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
        AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitLoggerExtension.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        LOG.info("Running @BeforeAll for " + context.getRequiredTestClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.info("Running @BeforeEach for " + context.getRequiredTestClass().getName()
                + " : " + context.getDisplayName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        LOG.info(context.getDisplayName() + ": Running.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        LOG.info(context.getDisplayName() + ": Execution finished.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterEach(ExtensionContext context) {
        LOG.info("Running @AfterEach for "
                + context.getRequiredTestClass().getName()
                + " : " + context.getDisplayName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterAll(ExtensionContext context) {
        LOG.info("Running @AfterAll for " + context.getRequiredTestClass().getName());
    }
}
