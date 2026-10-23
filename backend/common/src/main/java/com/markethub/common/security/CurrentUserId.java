package com.markethub.common.security;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
    /** 是否必须（默认为true，如果未登录则抛异常） */
    boolean required() default true;
}
