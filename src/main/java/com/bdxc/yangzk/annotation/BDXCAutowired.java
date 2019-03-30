package com.bdxc.yangzk.annotation;

import java.lang.annotation.*;

/**
 * dispathcherServletDemo
 * Yangzk
 * 2019/3/27
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BDXCAutowired {
    String value() default "";
}
