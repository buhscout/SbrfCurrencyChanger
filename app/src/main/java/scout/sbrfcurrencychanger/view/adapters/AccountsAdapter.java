package scout.sbrfcurrencychanger.view.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.Account;

/**
 * Адаптер для счёта
 */
public class AccountsAdapter extends ArrayAdapter<Account> {

    private int mLayoutResourceId;

	public AccountsAdapter(Context context, int layoutResourceId) {
		super(context, layoutResourceId, new ArrayList<Account>());
		mLayoutResourceId = layoutResourceId;
	}

	public AccountsAdapter(Context context, int layoutResourceId, Account[] data) {
		super(context, layoutResourceId, data);
		mLayoutResourceId = layoutResourceId;
	}

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        AccountHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);

            holder = new AccountHolder();
            holder.CurrencyIcon = (ImageView) row.findViewById(R.id.accountIcon);
            holder.Name = (TextView) row.findViewById(R.id.accountName);
			holder.Number = (TextView) row.findViewById(R.id.accountNumber);
            holder.Balance = (TextView) row.findViewById(R.id.accountBalance);
            row.setTag(holder);
        } else {
            holder = (AccountHolder) row.getTag();
        }
        Account account = getItem(position);
        holder.Name.setText(account.getName());
		holder.Number.setText(account.getAccountNumber());

		holder.Balance.setText(Repository.getDecimalFormat().format(account.getBalance()) + " " + account.getCurrency().getSymbol());
        switch (account.getCurrency().getCode()) {
            case "RUB":
                holder.CurrencyIcon.setImageResource(R.drawable.currency_rub);
                break;
            case "USD":
                holder.CurrencyIcon.setImageResource(R.drawable.currency_usd);
                break;
            case "EUR":
                holder.CurrencyIcon.setImageResource(R.drawable.currency_eur);
                break;
        }
        return row;
    }

    public static class AccountHolder {
        public ImageView CurrencyIcon;
        public TextView Name;
        public TextView Number;
        public TextView Balance;
    }
}
