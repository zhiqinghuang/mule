/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.HasPoolingProfile;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate}
 * with a user configured {@link PoolingProfile} or the default one if is was not supplied by the user.
 * <p/>
 * The purpose of this wrapper is having the {@link #getHandlingStrategy(ConnectionHandlingStrategyFactory)}
 * method use the configured {@link #poolingProfile} instead of the default included
 * in the {@link #delegate}
 * <p/>
 *
 * @since 4.0
 */
public final class PooledConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection> implements Lifecycle, HasPoolingProfile
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PooledConnectionProviderWrapper.class);
    private final PoolingProfile poolingProfile;
    private final boolean disableValidation;
    private final RetryPolicyTemplate retryPolicyTemplate;

    @Inject
    MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param delegate            the {@link ConnectionProvider} to be wrapped
     * @param poolingProfile      a not {@code null} {@link PoolingProfile}
     * @param retryPolicyTemplate a {@link AbstractPolicyTemplate} which will hold the retry policy configured in the Mule Application
     */
    public PooledConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate,
                                           PoolingProfile poolingProfile,
                                           boolean disableValidation,
                                           RetryPolicyTemplate retryPolicyTemplate)
    {
        super(delegate);
        this.poolingProfile = poolingProfile;
        this.disableValidation = disableValidation;
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    /**
     * Delegates the responsibility of validating the connection to the delegated {@link ConnectionProvider}
     * If {@link #disableValidation} if {@code true} then the validation is skipped, returning {@link ConnectionValidationResult#success()}
     *
     * @param connection a given connection
     * @return A {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
     */
    @Override
    public ConnectionValidationResult validate(Connection connection)
    {
        if (disableValidation)
        {
            return ConnectionValidationResult.success();
        }
        return getDelegate().validate(connection);
    }

    /**
     * @return a {@link RetryPolicyTemplate} with the configured values in the Mule Application.
     */
    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(retryPolicyTemplate, LOGGER);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(retryPolicyTemplate, true, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(retryPolicyTemplate);
    }

    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(retryPolicyTemplate);
    }

    @Override
    public Optional<PoolingProfile> getPoolingProfile()
    {
        return Optional.ofNullable(poolingProfile);
    }
}
