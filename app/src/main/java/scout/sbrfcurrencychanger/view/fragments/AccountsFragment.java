package scout.sbrfcurrencychanger.view.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListView;

import scout.sbrfcurrencychanger.OnChangeListener;
import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.Account;
import scout.sbrfcurrencychanger.view.adapters.AccountsAdapter;

/**
 * Фрагмент для списка счетов
 */
public class AccountsFragment extends Fragment {
	private static final String STATE_SCROLL_POSITION_X = "scrollX";
	private static final String STATE_SCROLL_POSITION_Y = "scrollY";
	private View mView;
	private OnChangeListener<Account> onChangeAccountListener;
	private boolean mNeedRefresh = false;

	public AccountsFragment() {
		super();
		onChangeAccountListener = new OnChangeListener<Account>() {
			@Override
			public void OnChange(Account item) {
				mNeedRefresh = true;
				Activity activity = getActivity();
				if(activity == null) {
					return;
				}
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View listView = mView.findViewById(R.id.accountsFragment_lvAccounts);
						if (!(listView instanceof ListView)) {
							return;
						}
						Adapter adapter = ((ListView) listView).getAdapter();
						if (adapter instanceof AccountsAdapter) {
							((AccountsAdapter) adapter).notifyDataSetChanged();
						}
					}
				});
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		if(mNeedRefresh) {
			mNeedRefresh = false;
			getFragmentManager().beginTransaction().detach(this).attach(this).commit();
		}
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.fragment_accounts, container, false);
		Repository.initialize(mView.getContext());
		if(savedInstanceState != null) {
			mView.scrollTo(savedInstanceState.getInt(STATE_SCROLL_POSITION_X), savedInstanceState.getInt(STATE_SCROLL_POSITION_Y));
		}
        AccountsAdapter adapter = new AccountsAdapter(mView.getContext(), R.layout.listview_account_row, Repository.getActiveAccounts());
		((ListView)mView.findViewById(R.id.accountsFragment_lvAccounts)).setAdapter(adapter);

		Repository.addOnSaveAccountListener(onChangeAccountListener);
        return mView;
    }

	@Override
	public void onDestroy() {
		Repository.removeOnSaveAccountListener(onChangeAccountListener);

		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_SCROLL_POSITION_X, mView.getScrollX());
		outState.putInt(STATE_SCROLL_POSITION_Y, mView.getScrollY());
		super.onSaveInstanceState(outState);
	}
}
