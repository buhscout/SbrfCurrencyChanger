package scout.sbrfcurrencychanger;

/**
 * Слушатель изменения
 */
public interface OnChangeListener<T> {

	/**
	 * Изменение
	 * @param item Изменённый элемент
	 */
	void OnChange(T item);

}
