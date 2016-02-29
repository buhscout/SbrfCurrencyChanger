package org.objectsqlite;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by goncharovka on 16.07.2014.
 */
public class ReflectionHelper {

	/**
	 * SQL тип поля класса
	 * @param field Поле класса
	 */
	public static String getFieldSqlType(Field field) {
		Class type = field.getType();
		if(type.isAnnotationPresent(SqlTable.class)) {
			Field keyField = getKeyField(type);
			if(keyField == null) {
				return null;
			}
			return getFieldSqlType(keyField);
		}
		if(type == Byte.class || type == Byte.TYPE
				|| type == Short.class || type == Short.TYPE
				|| type == Integer.class || type == Integer.TYPE
				|| type == Long.class || type == Long.TYPE
				|| type == Boolean.class || type == Boolean.TYPE
				|| type == Character.class || type == Character.TYPE
				|| type == Date.class) {
			return "INTEGER";
		}
		if(type == Float.class || type == Float.TYPE || type == Double.class || type == Double.TYPE) {
			return "REAL";
		}
		if(type == String.class) {
			return "TEXT";
		}
		if(type == Byte[].class) {
			return "BLOB";
		}
		throw new IllegalArgumentException(String.format("Тип '%s' поля '%s' не поддерживается", type.getName(), field.getName()));
	}

	/**
	 * Значение ключевого поля
	 * @param entity Элемент, в котором искать ключ
	 */
	public static Object getKeyValue(Object entity) {
		if (entity == null) {
			return null;
		}
		if(entity.getClass() == Date.class) {
			return ((Date)entity).getTime();
		}
		Class type = entity.getClass();
		if (!type.isAnnotationPresent(SqlTable.class)) {
			return entity;
		}
		Field field = getKeyField(type);
		if (field == null) {
			return entity;
		}
		field.setAccessible(true);
		Object value;
		try {
			value = field.get(entity);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		return field.getType().isAnnotationPresent(SqlTable.class) ? getKeyValue(value) : value;
	}

	/**
	 * Найти ключевое поле
	 * @param type Тип в котором искать ключевое поле
	 */
	public static Field getKeyField(Class type) {
		if (type == null) {
			throw new IllegalArgumentException("Тип в котором искать ключевое поле не задан");
		}
		if (!type.isAnnotationPresent(SqlTable.class)) {
			throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", type.getName()));
		}
		for (Field field : type.getDeclaredFields()) {
			if (!field.isAnnotationPresent(SqlPrimaryKey.class)) {
				continue;
			}
			return field;
		}
		return null;
	}

	/**
	 * Поля класса, помеченные для работы с таблицей
	 * @param type Тип класса
	 */
	public static Map<String, Field> getClassFields(Class type) {
		if (type == null) {
			throw new IllegalArgumentException("Тип класса не задан");
		}
		Map<String, Field> fields = new Hashtable<>();
		if(!type.isAnnotationPresent(SqlTable.class)) {
			return fields;
		}
		for (Field field : type.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			SqlColumn sqlColumnAnnotation = field.getAnnotation(SqlColumn.class);
			if (sqlColumnAnnotation != null) {
				String name = sqlColumnAnnotation.name() != null && !sqlColumnAnnotation.name().equals("")
						? sqlColumnAnnotation.name()
						: getFieldName(field);
				fields.put(name, field);
				continue;
			}
			SqlPrimaryKey pkAnnotation = field.getAnnotation(SqlPrimaryKey.class);
			if (pkAnnotation != null) {
				String name = pkAnnotation.name() != null && !pkAnnotation.name().equals("")
						? pkAnnotation.name()
						: getFieldName(field);
				fields.put(name, field);
			}
		}
		return fields;
	}

	/**
	 * Имя поля, без учёта имени в аннотации
	 * @param field Поле
	 */
	private static String getFieldName(Field field) {
		if(field == null) {
			throw new IllegalArgumentException("Поле не задано");
		}
		String fName = field.getName();
		if(fName.charAt(0) == 'm') {
			fName = fName.substring(1);
		}
		return fName;
	}

	/**
	 * Имя поля, с учётом имени в аннотации
	 * @param field Поле
	 */
	public static String getAnnotatedFieldName(Field field) {
		SqlPrimaryKey pkAnnotation = field.getAnnotation(SqlPrimaryKey.class);
		if (pkAnnotation != null && pkAnnotation.name() != null && !pkAnnotation.name().equals("")) {
			return pkAnnotation.name();
		}
		SqlColumn sqlColumnAnnotation = field.getAnnotation(SqlColumn.class);
		if (sqlColumnAnnotation != null && sqlColumnAnnotation.name() != null && !sqlColumnAnnotation.name().equals("")) {
			return sqlColumnAnnotation.name();
		}
		return pkAnnotation == null && sqlColumnAnnotation == null ? null : getFieldName(field);
	}

	/**
	 * Имя таблицы класса, с учётом имени в аннотации
	 * @param type Класс
	 */
	public static String getTableName(Class type) {
		if(type == null) {
			throw new IllegalArgumentException("Класс не задан");
		}
		Annotation annotation = type.getAnnotation(SqlTable.class);
		if(annotation == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", type.getName()));
		}
		if(((SqlTable)annotation).name() != null && !((SqlTable)annotation).name().equals("")) {
			return ((SqlTable)annotation).name();
		}
		String name = type.getSimpleName();
		if(name.charAt(name.length() - 1) == 'y') {
			name = name.substring(0, name.length() - 1) + "ie";
		}
		return name + "s";
	}

}
