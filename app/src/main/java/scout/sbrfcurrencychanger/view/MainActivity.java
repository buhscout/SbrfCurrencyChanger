package scout.sbrfcurrencychanger.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.htmlcleaner.XPatherException;

import java.util.HashMap;
import java.util.Map;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.dao.WebDao;
import scout.sbrfcurrencychanger.service.ExchangeReceiver;
import scout.sbrfcurrencychanger.service.ExchangeService;
import scout.sbrfcurrencychanger.view.adapters.DrawerAdapter;
import scout.sbrfcurrencychanger.view.fragments.AccountsFragment;
import scout.sbrfcurrencychanger.view.fragments.CurrencyRateHistoryFragment;
import scout.sbrfcurrencychanger.view.fragments.ExchangeHistoryFragment;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {


	private static final String BUNDLE_DRAWER_POSITION = "navigationPosition";
	private static final String BUNDLE_DRAWER_IS_OPENED = "isChangeFragment";
	private static final int DRAWER_POSITION_ACCOUNTS = 0;
	private static final int DRAWER_POSITION_HISTORY = 1;
	private static final int DRAWER_POSITION_HISTORY_CURRENCY_RATE = 2;
	private static final int DRAWER_POSITION_HISTORY_EXCHANGE = 3;
	private static final int DRAWER_POSITION_SETTINGS = 4;

	private int mDrawerPosition = DRAWER_POSITION_ACCOUNTS;
	private boolean mDrawerIsOpened = false;
	private DrawerAdapter mDrawerAdapter;
	private DrawerLayout mDrawerLayout;
	private LinearLayout mDrawerContentLayout;
	private TextView tvLogin;
	//private int mNewHistoryCounter;
	private String[] mDrawerTitles;
	private Map<Integer, Integer> mDrawerImages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Repository.initialize(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

		mDrawerImages = new HashMap<>();
		mDrawerImages.put(DRAWER_POSITION_ACCOUNTS,  R.drawable.ic_list_gray);
		mDrawerImages.put(DRAWER_POSITION_HISTORY_EXCHANGE, R.drawable.ic_exchange);
		mDrawerImages.put(DRAWER_POSITION_HISTORY_CURRENCY_RATE, R.drawable.ic_recent_gray);
		mDrawerImages.put(DRAWER_POSITION_SETTINGS, R.drawable.ic_settings);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.activity_main_drawerLayout);
		mDrawerContentLayout = (LinearLayout)mDrawerLayout.findViewById(R.id.activity_main_drawerContentLayout);
		ListView drawerList = (ListView) mDrawerContentLayout.findViewById(R.id.activity_main_lvDrawer);
		tvLogin = (TextView)mDrawerContentLayout.findViewById(R.id.activity_main_tvUserName);
		mDrawerTitles = getResources().getStringArray(R.array.drawer_titles);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setIcon(R.drawable.ic_launcher);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
		if (drawerList != null) {
			mDrawerAdapter = getDrawerAdapter();
			drawerList.setOnItemClickListener(new DrawerItemClickListener());
			drawerList.setAdapter(mDrawerAdapter);
		}
		if (savedInstanceState != null) {
			onRestoreInstanceState(savedInstanceState);
		} else {
			setFragmentList(mDrawerPosition);
		}
		mDrawerAdapter.invertCheckState(mDrawerPosition);
		setTitleFragments(mDrawerPosition);
		onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(this), getString(R.string.preferences_login));
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		new ExchangeReceiver().onReceive(this, new Intent(this, ExchangeReceiver.class));
/*
		try {
			new WebDao(this).getAccounts();
			new WebDao(this).ChangeCurrency(Repository.getAccounts()[1], Repository.getMainAccount(), 1, 50);
		} catch (WebDao.SbrfException e) {
			e.printStackTrace();
		}*/
		//Account acc = Repository.getAccounts()[0];
		//acc.setBalance(100);
		//Repository.saveAccount(acc);

		/*Currency cur0 = new Currency("RUB", "руб.");
		SQLiteDao.save(cur0);
		Currency[] curs = SQLiteDao.get(Currency.class);
		Currency curItem = SQLiteDao.get(Currency.class, curs[0].getCode());
		SQLiteDao.save(new Account(curItem, "1" , "Рублёвый", "cards=1"));
		Account acc = SQLiteDao.save(new Account(Repository.getCurrencies()[1],"2", "Долларовый", "cards=2"));
		SQLiteDao.save(curItem);
		cur0 = SQLiteDao.get(Currency.class, acc.getCurrency().getCode());
		acc = SQLiteDao.get(Account.class, acc.getHref());
		Account[] accs = SQLiteDao.get(Account.class);
        */
	}

	//public int getNewHistoryCounter() {
	//	return mNewHistoryCounter;
	//}

	//public void setNewHistoryCounter(int value) {
	//	mNewHistoryCounter = value;
	//}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem refreshButton = menu.findItem(R.id.action_refresh);
		switch (mDrawerPosition) {
			case DRAWER_POSITION_ACCOUNTS:
				refreshButton.setVisible(true);
				break;
			case DRAWER_POSITION_HISTORY:
				refreshButton.setVisible(false);
				break;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        stopService(new Intent(this, ExchangeService.class));
		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mDrawerPosition = savedInstanceState.getInt(BUNDLE_DRAWER_POSITION);
		mDrawerIsOpened = savedInstanceState.getBoolean(BUNDLE_DRAWER_IS_OPENED);
		if(mDrawerIsOpened && !mDrawerLayout.isDrawerOpen(mDrawerContentLayout)) {
			mDrawerLayout.openDrawer(mDrawerContentLayout);
		} else if(!mDrawerIsOpened && mDrawerLayout.isDrawerOpen(mDrawerContentLayout)) {
			mDrawerLayout.closeDrawer(mDrawerContentLayout);
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(BUNDLE_DRAWER_POSITION, mDrawerPosition);
		outState.putBoolean(BUNDLE_DRAWER_IS_OPENED, mDrawerIsOpened);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mDrawerLayout.isDrawerOpen(mDrawerContentLayout)) {
					mDrawerLayout.closeDrawer(mDrawerContentLayout);
					mDrawerIsOpened = false;
				} else {
					mDrawerLayout.openDrawer(mDrawerContentLayout);
					mDrawerIsOpened = true;
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		if (getActionBar() != null) {
			getActionBar().setTitle(title);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.preferences_login))) {
			tvLogin.setText(sharedPreferences.getString(key, ""));
		}
	}

	public DrawerAdapter getDrawerAdapter() {
		DrawerAdapter drawerAdapter = new DrawerAdapter(this);
		drawerAdapter.setCheckedIsSingle(true);
		if (mDrawerTitles.length == 0) {
			return drawerAdapter;
		}
		for (int i = 0; i < mDrawerTitles.length; i++) {
			NavigationItem itemNavigation;
			switch (i) {
				case DRAWER_POSITION_HISTORY:
					itemNavigation = new NavigationItem(mDrawerTitles[i], 0, true);
					break;
				case DRAWER_POSITION_SETTINGS:
					itemNavigation = new NavigationItem(mDrawerTitles[i], mDrawerImages.get(i), getResources().getColor(R.color.gray));
					break;
				default:
					itemNavigation = new NavigationItem(mDrawerTitles[i], mDrawerImages.get(i), null);
					break;
			}
			drawerAdapter.addItem(itemNavigation);
		}
		return drawerAdapter;
	}

	private void setFragmentList(int position) {
		Fragment fragment = null;
		switch (position) {
            case DRAWER_POSITION_ACCOUNTS:
				fragment = new AccountsFragment();
                break;
			case DRAWER_POSITION_HISTORY_EXCHANGE:
				fragment = new ExchangeHistoryFragment();
				break;
			case DRAWER_POSITION_HISTORY_CURRENCY_RATE:
				fragment = new CurrencyRateHistoryFragment();
				break;
		}
		if(fragment == null) {
			return;
		}
		invalidateOptionsMenu();
		getFragmentManager().beginTransaction().replace(R.id.activity_main_frameContent, fragment).commitAllowingStateLoss();
	}

	private void setTitleFragments(int position) {
		ActionBar actionBar = getActionBar();
		if (actionBar == null) {
			return;
		}
		actionBar.setIcon(mDrawerImages.get(position));
		actionBar.setSubtitle(mDrawerTitles[position]);
	}

	public void onRefreshMenuItemClick(MenuItem item) throws WebDao.SbrfException {
		WaitDialog.execute(this, new WaitDialog.WaitDialogTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (!Repository.getSbrfDao().isLoggedIn()) {
                        setMessage("Подключение к серверу...");
                        Repository.getSbrfDao().Login();
                    }
                    setMessage("Обновление счетов...");
                    Repository.refreshAccounts();
                } catch (WebDao.SbrfException | XPatherException e1) {
                    e1.printStackTrace();
                    cancel(true);
                }
                return null;
            }
        });
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
			if (position == DRAWER_POSITION_SETTINGS) {
				mDrawerLayout.closeDrawer(mDrawerContentLayout);
				mDrawerIsOpened = false;
				startActivity(new Intent(view.getContext(), PreferencesActivity.class));
			} else {
				mDrawerPosition = position;
				mDrawerAdapter.invertCheckState(position);
				mDrawerLayout.closeDrawer(mDrawerContentLayout);
				mDrawerIsOpened = false;
				new DrawFragmentTask().execute();
			}
		}
	}

	class DrawFragmentTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			try {
				Thread.sleep(300);
				setFragmentList(mDrawerPosition);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			setTitleFragments(mDrawerPosition);
		}
	}

}
