package scout.sbrfcurrencychanger.view;

/**
 * Элемент меню
 */
public class NavigationItem {
	private String mTitle;
	private int mCounter;
	private int mIcon;
	private boolean mIsHeader;
	private Integer mColor;

    public NavigationItem(String title, int icon, boolean header, int counter, Integer color) {
        mTitle = title;
        mIcon = icon;
        mIsHeader = header;
        mCounter = counter;
		mColor = color;
    }

    public NavigationItem(String title, int icon, boolean header) {
        this(title, icon, header, 0, null);
    }

    public NavigationItem(String title, int icon, Integer color) {
        this(title, icon, false, 0, color);
    }

	public String getTitle() {
		return mTitle;
	}

	public int getCounter() {
		return mCounter;
	}

	public int getIcon() {
		return mIcon;
	}

	public boolean isHeader() {
		return mIsHeader;
	}

	public Integer getColor() {
		return mColor;
	}
}
