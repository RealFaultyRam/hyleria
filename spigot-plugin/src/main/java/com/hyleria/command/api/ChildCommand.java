package com.hyleria.command.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ben (OutdatedVersion)
 * @since Feb/28/2017 (6:02 PM)
 */
@Retention ( RetentionPolicy.RUNTIME )
@Target ( ElementType.FIELD )
public @interface ChildCommand
{

}
