package it.pagopa.pm.gateway.utils;

import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryerCustom implements Retryer {

    private final int maxAttempts;
    private final long period;
    private final long maxPeriod;
    int attempt;
    long sleptForMillis;

    public RetryerCustom(long period, long maxPeriod, int maxAttempts) {
        this.period = period;
        this.maxPeriod = maxPeriod;
        this.maxAttempts = maxAttempts;
        this.attempt = 1;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public void continueOrPropagate(RetryableException e) {
        if (this.attempt++ >= this.maxAttempts) {
            throw e;
        } else {
            log.info(String.format("Attempt no.%s", attempt));
            long interval;
            if (e.retryAfter() != null) {
                interval = e.retryAfter().getTime() - this.currentTimeMillis();
                if (interval > this.maxPeriod) {
                    interval = this.maxPeriod;
                }

                if (interval < 0L) {
                    return;
                }
            } else {
                interval = this.nextMaxInterval();
            }

            try {
                Thread.sleep(interval);
            } catch (InterruptedException var5) {
                Thread.currentThread().interrupt();
                throw e;
            }

            this.sleptForMillis += interval;
        }
    }

    long nextMaxInterval() {
        long interval = (long) (this.period * Math.pow(1.5D, (this.attempt - 1)));
        return Math.min(interval, this.maxPeriod);
    }

    @SuppressWarnings({"squid:S1182", "squid:S2975"})
    public Retryer clone() {
        return new RetryerCustom(this.period, this.maxPeriod, this.maxAttempts);
    }
}

