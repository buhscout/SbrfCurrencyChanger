package scout.sbrfcurrencychanger.view.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.CurrencyRate;

/**
 * Адаптер для счёта
 */
public class CurrencyRatesAdapter extends ArrayAdapter<CurrencyRate> {

    private int mLayoutResourceId;
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
	private static final Map<String, Integer> sCurrencyImages = new HashMap<String, Integer>()
	{
		{
			put("RUB", R.drawable.currency_rub);
			put("USD", R.drawable.currency_usd);
			put("EUR", R.drawable.currency_eur);
		}
	};

	public CurrencyRatesAdapter(Context context, int layoutResourceId, CurrencyRate[] data) {
		super(context, layoutResourceId, Sort(data));

		mLayoutResourceId = layoutResourceId;
	}

	/**
	 * Сортировка
	 * @param data Массив для сортировки
	 */
	private static ArrayList<CurrencyRate> Sort(CurrencyRate[] data){
		ArrayList<CurrencyRate> sortedData = new ArrayList<>();
		Collections.addAll(sortedData, data);
		CurrencyRate temp;
		for(int i = 0; i < sortedData.size(); i++) {
			for (int j = i; j < sortedData.size(); j++) {
				if(sortedData.get(j).getDate().after(sortedData.get(i).getDate())) {
					temp = sortedData.get(j);
					sortedData.set(j, sortedData.get(i));
					sortedData.set(i, temp);
				}
			}
		}
		return sortedData;
	}

	@Override
	public void addAll(CurrencyRate... items) {
		super.addAll(Sort(items));
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		CurrencyRateHolder holder;
		CurrencyRate currencyRate = getItem(position);
		LayoutInflater layoutInflater = ((Activity) getContext()).getLayoutInflater();

		if (row == null) {
			row = layoutInflater.inflate(mLayoutResourceId, parent, false);
		}
		holder = (CurrencyRateHolder) row.getTag();
		if (holder == null) {
			holder = new CurrencyRateHolder();
			holder.CurrencyIcon = (ImageView) row.findViewById(R.id.currency_rate_row_image);
			holder.PurchasingChangeImage = (ImageView) row.findViewById(R.id.currency_rate_row_purchasing_change_image);
			holder.SellingChangeImage = (ImageView) row.findViewById(R.id.currency_rate_row_selling_change_image);
			holder.CurrencyCode = (TextView) row.findViewById(R.id.currency_rate_row_code);
			holder.SellingRate = (TextView) row.findViewById(R.id.currency_rate_row_selling);
			holder.PurchasingRate = (TextView) row.findViewById(R.id.currency_rate_row_purchasing);
			holder.SellingRateChange = (TextView) row.findViewById(R.id.currency_rate_row_selling_change);
			holder.PurchasingRateChange = (TextView) row.findViewById(R.id.currency_rate_row_purchasing_change);
			row.setTag(holder);
		}

		FrameLayout headerFrame = (FrameLayout) row.findViewById(R.id.currency_rate_row_header);
		if (position == 0 || !dateFormatter.format(getItem(position - 1).getDate()).equals(dateFormatter.format(currencyRate.getDate()))) {
			if (headerFrame.getChildCount() == 0) {
				headerFrame.addView(layoutInflater.inflate(R.layout.listview_currency_rate_header, parent, false));
				holder.HeaderName = (TextView) row.findViewById(R.id.tv_currencyRateHeaderName);
			}
		} else {
			headerFrame.removeAllViews();
			holder.HeaderName = null;
		}

		boolean isLastDate = true;
		for (int i = position + 1; i < getCount(); i++) {
			if(!getItem(i).getDate().equals(currencyRate.getDate()))
			{
				isLastDate = false;
				break;
			}
		}
		if(!isLastDate) {
			for (int i = position + 1; i < getCount(); i++) {
				CurrencyRate prevRate = getItem(i);
				if (prevRate.getCurrency().getCode().equals(currencyRate.getCurrency().getCode())) {
					float value = currencyRate.getPurchasingRate() - prevRate.getPurchasingRate();
					if(value > 0) {
						holder.PurchasingRateChange.setTextColor(Color.parseColor("#f711a406"));
						holder.PurchasingChangeImage.setImageResource(R.drawable.ic_arrow_up);
					} else if(value < 0) {
						holder.PurchasingRateChange.setTextColor(row.getResources().getColor(R.color.red_light));
						holder.PurchasingChangeImage.setImageResource(R.drawable.ic_arrow_down);
					} else {
						holder.PurchasingRateChange.setTextColor(row.getResources().getColor(R.color.black));
						holder.PurchasingChangeImage.setImageBitmap(null);
					}
					holder.PurchasingRateChange.setText(Repository.getDecimalFormat().format(value));

					value = currencyRate.getSellingRate() - prevRate.getSellingRate();
					if(value > 0) {
						holder.SellingRateChange.setTextColor(Color.parseColor("#f711a406"));
						holder.SellingChangeImage.setImageResource(R.drawable.ic_arrow_up);
					} else if(value < 0) {
						holder.SellingRateChange.setTextColor(row.getResources().getColor(R.color.red_light));
						holder.SellingChangeImage.setImageResource(R.drawable.ic_arrow_down);
					} else {
						holder.SellingRateChange.setTextColor(row.getResources().getColor(R.color.black));
						holder.SellingChangeImage.setImageBitmap(null);
					}
					holder.SellingRateChange.setText(Repository.getDecimalFormat().format(value));
					break;
				}
			}
		} else {
			holder.PurchasingRateChange.setTextColor(row.getResources().getColor(R.color.black));
			holder.SellingRateChange.setTextColor(row.getResources().getColor(R.color.black));
			holder.PurchasingRateChange.setText("0");
			holder.SellingRateChange.setText("0");
			holder.PurchasingChangeImage.setImageBitmap(null);
			holder.SellingChangeImage.setImageBitmap(null);
		}

		if(holder.HeaderName != null) {
			holder.HeaderName.setText(dateFormatter.format(currencyRate.getDate()));
		}
		holder.CurrencyCode.setText(currencyRate.getCurrency().getCode());
		holder.PurchasingRate.setText(Repository.getDecimalFormat().format(currencyRate.getPurchasingRate()));
		holder.SellingRate.setText(Repository.getDecimalFormat().format(currencyRate.getSellingRate()));
		holder.CurrencyIcon.setImageResource(sCurrencyImages.get(currencyRate.getCurrency().getCode()));
		return row;
	}

    public static class CurrencyRateHolder {
        public ImageView CurrencyIcon;
		public ImageView PurchasingChangeImage;
		public ImageView SellingChangeImage;
        public TextView HeaderName;
		public TextView CurrencyCode;
        public TextView SellingRate;
		public TextView SellingRateChange;
        public TextView PurchasingRate;
		public TextView PurchasingRateChange;
    }

}
