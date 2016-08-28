package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jesper on 2016-08-26.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface SQLiteDatabaseHolder {
    SQLiteDatabaseDescriptor[] databases();
}