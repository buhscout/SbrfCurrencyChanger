package org.objectsqlite.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Таблица
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SqlTable {
	/**
	 * Наименование
	 */
    String name() default "";

	/**
	 * Кэшируемый
	 */
	boolean cacheable() default false;
}
