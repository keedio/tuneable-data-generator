package org.keedio.datagenerator.domain;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 5/3/15.
 */
public interface RateLimiterSupervisorMBean {
    public double getPermitsPerSecond();
    public void setPermitsPerSecond(double permits);
}
