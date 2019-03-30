package com.bdxc.yangzk.annotation;

import java.lang.annotation.*;

/**
 * dispathcherServletDemo
 * Yangzk
 * 2019/3/27
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BDXCRequestMapping {
    String value();
}
