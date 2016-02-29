package scout.sbrfcurrencychanger.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.dao.WebDao;
import scout.sbrfcurrencychanger.entities.Account;
import scout.sbrfcurrencychanger.view.adapters.AccountsAdapter;

public class PreferencesActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(savedInstanceState == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
		}
		setUpActionBar(getActionBar());
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(1, new Intent());
            finish();
            return true;
        }
        return false;
    }

	/**
	 * Настройка панели дейстий
	 * @param actionBar Панель действий
	 */
	private static void setUpActionBar(ActionBar actionBar) {
		if (actionBar != null) {
			actionBar.setIcon(R.drawable.ic_settings_title);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
	}

	public static class PreferencesFragment extends PreferenceFragment implements
			SharedPreferences.OnSharedPreferenceChangeListener,
			CheckedListPreference.OnDialogClosedListener,
			CheckedListPreference.OnCheckedChangeListener,
			CheckedListPreference.OnShowDialogPreferenceListener {

		private CheckedListPreference mCheckedListPreference;
		private static final String BUNDLE_IS_DIALOG_SHOWN = "isDialogShown";
		private boolean isDialogShown = false;
		private static Account[] sAccounts;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences_screen);
			updateSummary(getPreferenceScreen());

			mCheckedListPreference = (CheckedListPreference)findPreference(getString(R.string.preferences_accounts));
			mCheckedListPreference.setOnDialogClosedListener(this);
			mCheckedListPreference.setOnCheckedChangeListener(this);
			mCheckedListPreference.setOnShowDialogListener(this);
			if(savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_IS_DIALOG_SHOWN)) {
				mCheckedListPreference.showDialog(null);
			}
		}

		@Override
		public void onPause() {
			PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

		@Override
		public void onResume() {
			super.onResume();
			PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onDestroy() {
			mCheckedListPreference.setOnDialogClosedListener(null);
			mCheckedListPreference.setOnCheckedChangeListener(null);
			mCheckedListPreference.setOnShowDialogListener(null);

			super.onDestroy();
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			//super.onSaveInstanceState(outState);
			outState.putBoolean(BUNDLE_IS_DIALOG_SHOWN, isDialogShown);
		}

		@Override
		public void onCheckedChanged(CheckedListPreference preference, int index, boolean isChecked) {
			if(!isChecked) {
				return;
			}
			List<Integer> checkedRows = preference.getCheckedRows();
			int[] checkedIndexes = new int[checkedRows.size()];
			for(int i = 0; i < checkedIndexes.length; i++) {
				checkedIndexes[i] = checkedRows.get(i);
			}
			for(int rowIndex : checkedIndexes) {
				if(rowIndex != index && ((Account)preference.getListAdapter().getItem(rowIndex)).getCurrency().equals(((Account)preference.getListAdapter().getItem(index)).getCurrency())) {
					preference.setRowCheckedState(rowIndex, false);
				}
			}
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			return super.onContextItemSelected(item);
		}

		@Override
		public void onShowDialog(final CheckedListPreference preference, CheckedListPreference.ShowDialogPreferenceEventArgs e) {
			ArrayAdapter adapter = preference.getListAdapter();
			if(adapter == null) {
				e.setCancel(true);
			} else {
				return;
			}
			if(!preference.getKey().equals(getString(R.string.preferences_accounts))){
				return;
			}
			final AccountsAdapter accountsAdapter = new AccountsAdapter(this.getPreferenceScreen().getContext(), R.layout.listview_account_row);
			preference.setListAdapter(accountsAdapter);
			if(sAccounts != null) {
				showDialog(preference, accountsAdapter, sAccounts);
				return;
			}

			WaitDialog.execute(getActivity(), new WaitDialog.WaitDialogTask<Void, Void, Account[]>() {
				@Override
				protected Account[] doInBackground(Void... voids) {
					try {
						if (!Repository.getSbrfDao().isLoggedIn()) {
							setMessage("Подключение к серверу...");
							Repository.getSbrfDao().Login();
						}
						setMessage("Загрузка счетов...");
						return Repository.getSbrfDao().getAccounts();
					} catch (WebDao.SbrfException e) {
						e.printStackTrace();
						this.cancel(true);
					}
					return new Account[0];
				}

				@Override
				protected void onPostExecute(Account[] accounts) {
					super.onPostExecute(accounts);
					sAccounts = accounts;
					showDialog(preference, accountsAdapter, accounts);
				}
			});
		}

		private void showDialog(CheckedListPreference preference, AccountsAdapter accountsAdapter, Account[] accounts) {
			accountsAdapter.addAll(accounts);
			Account[] activeAccounts = Repository.getActiveAccounts();
			if(activeAccounts.length > 0 && preference.getCheckedRows().size() == 0) {
				for (int i = 0; i < accounts.length; i++) {
					for (Account activeAccount : activeAccounts) {
						if (accounts[i].equals(activeAccount)) {
							preference.putCheckedRow(i);
						}
					}
				}
			}
			isDialogShown = true;
			preference.showDialog(preference.peekExtras());
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			updateSummary(findPreference(key));
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference) {
			super.onPreferenceTreeClick(preferenceScreen, preference);
			if (!(preference instanceof PreferenceScreen)) {
				return false;
			}
			final Dialog dialog = ((PreferenceScreen) preference).getDialog();
			if (dialog == null) {
				return false;
			}
			setUpActionBar(dialog.getActionBar());
			View homeBtn = dialog.findViewById(android.R.id.home);
			if (homeBtn == null) {
				return false;
			}
			View.OnClickListener dismissDialogClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					sAccounts = null;
				}
			};
			ViewParent homeBtnContainer = homeBtn.getParent();
			if (homeBtnContainer instanceof FrameLayout) {
				ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();
				if (containerParent instanceof LinearLayout) {
					containerParent.setOnClickListener(dismissDialogClickListener);
				} else {
					((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
				}
			} else {
				homeBtn.setOnClickListener(dismissDialogClickListener);
			}
			return false;
		}

		private void updateSummary(Preference preference) {
			if (preference instanceof PreferenceGroup) {
				PreferenceGroup group = (PreferenceGroup) preference;
				for (int i = 0; i < group.getPreferenceCount(); i++) {
					updateSummary(group.getPreference(i));
				}
			} else if (preference instanceof EditTextPreference) {
				EditTextPreference editText = (EditTextPreference) preference;
				int inputType = editText.getEditText().getInputType();
				if (((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD) || ((inputType & InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD) || ((inputType & InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
					return;
				}
				preference.setSummary(editText.getText());
			}
		}

		@Override
		public void onDialogClosed(CheckedListPreference preference, boolean isOk) {
			isDialogShown = false;
			if(!isOk) {
				return;
			}
			Set<String> accountIds = new HashSet<>();
			List<Account> accounts = new ArrayList<>();
			Collections.addAll(accounts, Repository.getAccounts());
			Adapter adapter = preference.getListAdapter();
			for(int rowIndex : preference.getCheckedRows()) {
				Account account = (Account)adapter.getItem(rowIndex);
				accountIds.add(account.getCode());
				Account srcAccount = null;
				for(int i = 0; i < accounts.size() && srcAccount == null; i++) {
					srcAccount = accounts.get(i);
					if(!srcAccount.equals(account)) {
						srcAccount = null;
					}
				}
				if(srcAccount != null) {
					accounts.remove(srcAccount);
					if (srcAccount.isActive()) {
						continue;
					}
					account = srcAccount;
				}
				account.setActive(true);
				Repository.saveAccount(account);
			}
			for(Account account : accounts) {
				if (account.isActive()) {
					account.setActive(false);
					Repository.saveAccount(account);
				}
			}
			PreferenceManager.getDefaultSharedPreferences(preference.getContext())
					.edit()
					.putStringSet(getString(R.string.preferences_accounts), accountIds)
					.commit();
		}

	}

}