package com.markethub.common.security;

import java.lang.annotation.*;

/** 权限校验注解，标注在 Controller 方法上。 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String[] value();

    /** 逻辑关系：AND 表示同时拥有所有权限，OR 表示拥有任一权限即可 */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}
