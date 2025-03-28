package com.aquiva.autotests.rc.internal;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy;

/**
 * Custom strategy for JUnit to orchestrate parallel test execution.
 * <br/>
 * Note: this is a workaround for Selenium 4 + JUnit 5 issue with parallelism.
 *
 * @see <a href='https://github.com/SeleniumHQ/selenium/issues/10113'>Selenium's issue #10113 on GitHub</a>
 */
public class CustomParallelExecutionConfigurationStrategy implements ParallelExecutionConfiguration, ParallelExecutionConfigurationStrategy {
    private static final Integer PARALLELISM = Integer.valueOf(System.getProperty("junit.parallelism"));

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParallelism() {
        return PARALLELISM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinimumRunnable() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPoolSize() {
        return PARALLELISM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCorePoolSize() {
        return PARALLELISM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getKeepAliveSeconds() {
        return 60;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParallelExecutionConfiguration createConfiguration(final ConfigurationParameters configurationParameters) {
        return this;
    }
}
