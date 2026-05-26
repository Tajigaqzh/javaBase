package com.hp.javabase.common.annotation;

import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";

    long rate() default 5;

    long rateInterval() default 1;

    RateIntervalUnit rateIntervalUnit() default RateIntervalUnit.SECONDS;

    RateType rateType() default RateType.OVERALL;

    boolean includeIp() default true;

    String message() default "too many requests";
}
