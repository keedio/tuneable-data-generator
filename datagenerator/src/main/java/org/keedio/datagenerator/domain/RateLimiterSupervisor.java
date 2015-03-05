package org.keedio.datagenerator.domain;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.Validate;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 5/3/15.
 */
public class RateLimiterSupervisor implements RateLimiterSupervisorMBean {
    private RateLimiter rateLimiter;

    public RateLimiterSupervisor(RateLimiter rateLimiter){
        this.rateLimiter = rateLimiter;

        String name = "org.keedio.datagenerator.domain:name=RateLimiterSupervisor";

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(name));
        } catch (MalformedObjectNameException |
                NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            throw new ExceptionInInitializerError();
        }
    }

    @Override
    public double getPermitsPerSecond() {
        return rateLimiter.getRate();
    }

    @Override
    public void setPermitsPerSecond(double permits) {
        Validate.notNull(permits);
        Validate.isTrue(permits > 0);

        rateLimiter.setRate(permits);
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}
