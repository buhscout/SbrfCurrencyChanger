package org.objectsqlite.Cache;

import android.os.Debug;
import android.util.Log;

import org.objectsqlite.Annotations.SqlTable;
import org.objectsqlite.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Кэш объектов
 */
public class ObjectCache {

	private static Map<Class, Map<Object, Object>> mObjects = new HashMap<>();

	/**
	 * Вставка объекта в кэш
	 * @param object
	 * 		Объект
	 */
	public static void put(Object object) {
		if(object == null) {
			return;
		}
		Class type = object.getClass();
		Annotation annotation = type.getAnnotation(SqlTable.class);
		if(annotation == null || !((SqlTable)annotation).cacheable()) {
			return;
		}
		Field keyField = ReflectionHelper.getKeyField(type);
		if(keyField == null) {
			return;
		}
		keyField.setAccessible(true);
		Object keyValue;
		try {
			keyValue = keyField.get(object);
		} catch (IllegalAccessException e) {
			return;
		}
		if(keyValue == null) {
			return;
		}
		Map<Object, Object> objects = mObjects.get(type);
		if(objects == null) {
			objects = new HashMap<>();
			mObjects.put(type, objects);
		}
		objects.put(keyValue, object);
		if(Debug.isDebuggerConnected()) {
			Log.v("ObjectCache", String.format("Object '%s' with id = '%s' put in cache", type.getName(), keyValue));
		}
	}

	/**
	 * Получение объекта из кэша
	 * @param type
	 * 		Тип класса
	 * @param keyValue
	 * 		Значение ключевого поля
	 */
	public static Object get(Class type, Object keyValue) {
		if(type == null || keyValue == null) {
			return null;
		}
		Map<Object, Object> objects = mObjects.get(type);
		if(objects == null) {
			return null;
		}
		Object object = objects.get(keyValue);
		if(object != null && Debug.isDebuggerConnected()) {
			Log.v("ObjectCache", String.format("Object '%s' with id = '%s' gets from cache", type.getName(), keyValue));
		}
		return object;
	}

	/**
	 * Удаление из кэша
	 * @param type
	 * 		Тип класса
	 * @param keyValue
	 * 		Значение ключевого поля
	 */
	public static void remove(Class type, Object keyValue) {
		if(type == null || keyValue == null) {
			return;
		}
		Map<Object, Object> objects = mObjects.get(type);
		if(objects == null) {
			return;
		}
		objects.remove(keyValue);
		if(objects.isEmpty()) {
			mObjects.remove(type);
		}
	}


}
