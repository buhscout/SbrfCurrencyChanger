package org.objectsqlite.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Первичный ключ таблицы
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SqlPrimaryKey {
	/**
	 * Наименование поля
	 */
    String name() default "";

	/**
	 * Признак автоинкремента (только Integer)
	 */
	boolean autoIncrement() default false;
}
