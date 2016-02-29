package org.objectsqlite;

import android.os.Debug;
import android.util.Pair;

import org.objectsqlite.Annotations.SqlTable;
import org.objectsqlite.Cache.QueryCache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Менеджер запросов
 */
public class QueryBuilder<T> {

	/**
	 * Справочник полей класса
	 */
	private Map<String, Field> mFields;

	/**
	 * Имя ключевого поля
	 */
	private String mPkName;

	/**
	 * Рабочий класс
	 */
	private Class<T> mGeneric;

	/**
	 * Конструктор
	 * @param generic Рабочий класс
	 */
	public QueryBuilder(Class<T> generic) {
		mGeneric = generic;
		initialize();
	}

	/**
	 * Рабочий класс
	 */
	public Class<T> getGeneric() {
		return mGeneric;
	}

	/**
	 * Справочник полей класса
	 */
	public Map<String, Field> getFields() {
		return mFields;
	}

	/**
	 * Запрос SELECT
	 */
	public List<Pair<String, String>> getCreate() {
		List<Pair<String, String>> tables = new ArrayList<>();
		String fields = "";
		String foreignKeys = "";
		for (Map.Entry<String, Field> field : mFields.entrySet()) {
			String fk = "";
			Class fieldType = field.getValue().getType();
			if (fieldType.isAnnotationPresent(SqlTable.class)) {
				tables.addAll(new QueryBuilder(fieldType).getCreate());
				fk += ",";
				Field fieldKeyField = ReflectionHelper.getKeyField(fieldType);
				if (fieldKeyField != null) {
					fk += String.format("FOREIGN KEY([%s]) REFERENCES [%s]([%s])", field.getKey(), ReflectionHelper.getTableName(fieldType), ReflectionHelper.getAnnotatedFieldName(fieldKeyField));
				}
			}
			if (!fields.isEmpty()) {
				fields += ",";
			}
			String sqlType = ReflectionHelper.getFieldSqlType(field.getValue());
			if (sqlType == null) {
				continue;
			}
			foreignKeys += fk;
			fields += String.format("[%s] %s", ReflectionHelper.getAnnotatedFieldName(field.getValue()), sqlType);
			if (field.getKey().equals(mPkName)) {
				fields += " PRIMARY KEY";
			}
		}
		String tableName = ReflectionHelper.getTableName(getGeneric());
		tables.add(new Pair<>(tableName, String.format("CREATE TABLE [%s] (%s%s);", tableName, fields, foreignKeys)));
		return tables;
	}

	/**
	 * Запрос SELECT с условиями WHERE
	 * @param whereConditions Условия WHERE
	 * @param recordsCount Количество строк
	 * @param orderByFields Условия сортировки
	 */
	public String getSelectWhere(int recordsCount, String orderByFields, String... whereConditions) {
		String query = "";
		for (String condition : whereConditions) {
			if (condition == null || condition.trim().isEmpty()) {
				continue;
			}
			query += (query.isEmpty() ? " WHERE " : " AND ") + condition;
		}
		query = getSelect() + query;
		if (orderByFields != null) {
			query += " ORDER BY " + orderByFields;
		}
		if (recordsCount > 0) {
			query += " LIMIT " + recordsCount;
		}
		return query;
	}

	/**
	 * Запрос SELECT
	 */
	public String getSelect() {
		String query = QueryCache.get(mGeneric);
		if (query == null) {
			String fields = "";
			String joins = "";
			String tableName = ReflectionHelper.getTableName(mGeneric);
			String mAlias = "_main_0";
			for (Map.Entry<String, Field> entry : ReflectionHelper.getClassFields(mGeneric).entrySet()) {
				Field field = entry.getValue();
				if (fields.length() > 0) {
					fields += ",";
				}
				fields += String.format("[%s].[%s] [0_%s]", mAlias, ReflectionHelper.getAnnotatedFieldName(field), field.getName());
				SqlTable annotation = field.getType().getAnnotation(SqlTable.class);
				if (annotation != null) {
					if(!annotation.cacheable()) {
						Pair<String, String> join = getJoin(field, mAlias, "", 1);
						if (join != null) {
							fields += "," + join.first;
							joins += join.second;
						}
					}
				}
			}
			query = String.format("SELECT %s FROM [%s] [%s]%s", fields, tableName, mAlias, joins);
			QueryCache.put(mGeneric, query);
		} else if (Debug.isDebuggerConnected()) {
			query = "/* From cache */ " + query;
		}
		return query;
	}

	/**
	 * Запрос SELECT по ключу
	 * @param keyValue Значение ключа
	 */
	public String getSelectByKey(Object keyValue) {
		if (mPkName == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", getGeneric().getName()));
		}
		if (keyValue == null) {
			throw new IllegalArgumentException("Значение ключа не задано");
		}
		return getSelect() + String.format(" WHERE [_main_0].[%s] = '%s';", mPkName, ReflectionHelper.getKeyValue(keyValue));
	}

	/**
	 * Запрос на существование
	 * @param entity Искомый элемент
	 */
	public String getExists(T entity) {
		if (mPkName == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", getGeneric().getName()));
		}
		if (entity == null) {
			throw new IllegalArgumentException("Искомый элемент не задан");
		}
		Field pkField = mFields.get(mPkName);
		pkField.setAccessible(true);
		try {
			return getExistsByKey(pkField.get(entity));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Запрос на существование по ключу
	 * @param keyValue
	 * 		Значение ключа
	 */
	public String getExistsByKey(Object keyValue) {
		if (mPkName == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", getGeneric().getName()));
		}
		if (keyValue == null) {
			throw new IllegalArgumentException("Значение ключа не задано");
		}
		return String.format("SELECT 1 FROM [%s] WHERE [%s] = '%s' LIMIT 1;", ReflectionHelper.getTableName(getGeneric()), mPkName, ReflectionHelper.getKeyValue(keyValue));
	}

	/**
	 * Запрос на сохранение
	 * @param entity Сохраняемый элемент
	 */
	public String getUpdate(T entity) {
		if (mPkName == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", getGeneric().getName()));
		}
		if (entity == null) {
			throw new IllegalArgumentException("Сохраняемый элемент не задан");
		}
		String query = "";
		for (Map.Entry<String, Field> entry : mFields.entrySet()) {
			if (mPkName != null && entry.getKey().equals(mPkName)) {
				continue;
			}
			entry.getValue().setAccessible(true);
			Object value;
			try {
				value = entry.getValue().get(entity);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			if (!query.equals("")) {
				query += ",";
			}
			value = ReflectionHelper.getKeyValue(value);
			if (value == null) {
				value = "NULL";
			} else {
				if (value.getClass() == Boolean.class || value.getClass() == Boolean.TYPE) {
					value = (boolean) value ? 1 : 0;
				}
				value = String.format("'%s'", value);
			}
			query += String.format("[%s] = %s", entry.getKey(), value);
		}
		return String.format("UPDATE [%s] SET %s WHERE [%s] = '%s';", ReflectionHelper.getTableName(getGeneric()), query, mPkName, ReflectionHelper.getKeyValue(entity));
	}

	/**
	 * Запрос на удаление
	 * @param entity Удаляемый элемент
	 */
	public String getDelete(T entity) {
		if (mPkName == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", getGeneric().getName()));
		}
		if (entity == null) {
			throw new IllegalArgumentException("Сохраняемый элемент не задан");
		}
		return String.format("DELETE FROM [%s] WHERE [%s] = '%s';", ReflectionHelper.getTableName(getGeneric()), mPkName, ReflectionHelper.getKeyValue(entity));
	}

	/**
	 * Запрос на вставку
	 * @param entity Элемент для вставки
	 */
	public String getInsert(T entity) {
		String fields = "";
		String values = "";
		for (Map.Entry<String, Field> entry : mFields.entrySet()) {
			entry.getValue().setAccessible(true);
			Object value;
			try {
				value = entry.getValue().get(entity);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			if (mPkName != null && entry.getKey().equals(mPkName) && value == null) {
				continue;
			}
			if (!fields.equals("")) {
				fields += ",";
				values += ",";
			}
			fields += String.format("[%s]", entry.getKey());
			value = ReflectionHelper.getKeyValue(value);
			if(value == null) {
				values += "NULL";
			}
			else {
				if (value.getClass() == Boolean.class || value.getClass() == Boolean.TYPE) {
					value = (boolean) value ? 1 : 0;
				}
				values += String.format("'%s'", value);
			}
		}
		return String.format("INSERT INTO [%s] (%s) VALUES (%s);", ReflectionHelper.getTableName(getGeneric()), fields, values);
	}

	/**
	 * Инициализация
	 */
	private void initialize() {
		if (mFields != null) {
			return;
		}
		if (!getGeneric().isAnnotationPresent(SqlTable.class)) {
			throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", getGeneric().getName()));
		}
		mFields = ReflectionHelper.getClassFields(getGeneric());
		if (mFields.isEmpty()) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlColumn", getGeneric().getName()));
		}
		Field pkField = ReflectionHelper.getKeyField(getGeneric());
		if (pkField != null) {
			mPkName = ReflectionHelper.getAnnotatedFieldName(pkField);
		}
	}

	/**
	 * Часть запроса SELECT JOIN
	 */
	private Pair<String, String> getJoin(Field field, String parentAlias, String path, int deepLevel) {
		String fieldName = ReflectionHelper.getAnnotatedFieldName(field);
		if (fieldName == null) {
			return null;
		}
		Class fieldType = field.getType();
		Field keyField = ReflectionHelper.getKeyField(fieldType);
		if (keyField == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не содержит полей с аннотацией @SqlPrimaryKey", fieldType.getName()));
		}
		String fields = "";
		String tableName = ReflectionHelper.getTableName(fieldType);
		String alias = String.format("%s%s_%s", deepLevel, path, fieldName);
		path += "_" + fieldName;
		String query = String.format(" LEFT JOIN [%s] [%s] ON [%s].[%s] = [%s].[%s]", tableName, alias, alias, ReflectionHelper.getAnnotatedFieldName(keyField), parentAlias, fieldName);

		for (Map.Entry<String, Field> entry : ReflectionHelper.getClassFields(fieldType).entrySet())  {
			if (!fields.isEmpty()) {
				fields += ",";
			}
			fields += String.format("[%s].[%s] [%s_%s]", alias, entry.getKey(), alias, entry.getValue().getName());
			if (entry.getValue().getType().isAnnotationPresent(SqlTable.class)) {
				Pair<String, String> join = getJoin(entry.getValue(), alias, path, deepLevel + 1);
				if (join != null) {
					fields += "," + join.first;
					query += join.second;
				}
			}
		}
		return new Pair<>(fields, query);
	}

}
