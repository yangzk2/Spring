package com.bdxc.yangzk.annotation;

import java.lang.annotation.*;

/**
 * dispathcherServletDemo
 * Yangzk
 * 2019/3/27
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BDXCRequestParam {
    String value();
}
