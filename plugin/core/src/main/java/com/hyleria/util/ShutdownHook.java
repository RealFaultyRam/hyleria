package com.hyleria.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

 /**
  * @author Ben (OutdatedVersion)
  * @since Dec/11/2016 (6:51 PM)
  */
@Retention ( RetentionPolicy.RUNTIME )
@Target ( ElementType.METHOD )
public @interface ShutdownHook
{

    // maybe some before/after parameter
    // they'd make sure that this hook is
    // executed before/after the specified one

}
