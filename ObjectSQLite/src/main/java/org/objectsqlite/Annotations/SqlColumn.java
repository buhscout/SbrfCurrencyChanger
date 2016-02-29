package org.objectsqlite.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Поле таблицы
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SqlColumn {
	/**
	 * Наименование поля
	 */
	String name() default "";

	/**
	 * Каскадное сохранение (UPDATE)
	 */
	boolean cascadeUpdate() default false;

	/**
	 * Каскадное сохранение (INSERT)
	 */
	boolean cascadeInsert() default true;
}
