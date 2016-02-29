package scout.sbrfcurrencychanger.view.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.Exchange;

/**
 * Класс адаптера листа с историей обмена
 */
public class ExchangeAdapter extends ArrayAdapter<Exchange> {

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

	public ExchangeAdapter(Context context, int layoutResourceId, List<Exchange> data) {
		super(context, layoutResourceId, data);

		mLayoutResourceId = layoutResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder holder;
		Exchange exchange = getItem(position);
		LayoutInflater layoutInflater = ((Activity) getContext()).getLayoutInflater();

		if (row == null) {
			row = layoutInflater.inflate(mLayoutResourceId, parent, false);
		}
		holder = (ViewHolder) row.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.tvSrcName = (TextView) row.findViewById(R.id.tv_exchange_row_source_name);
			holder.ivSrcImage = (ImageView) row.findViewById(R.id.iv_exchange_row_source_image);
			holder.tvDstName = (TextView) row.findViewById(R.id.tv_exchange_row_destination_name);
			holder.ivDstImage = (ImageView) row.findViewById(R.id.iv_exchange_row_destination_image);
			holder.tvRate = (TextView) row.findViewById(R.id.tv_exchange_row_rate);
			holder.tvValue = (TextView) row.findViewById(R.id.tv_exchange_row_value);
			holder.tvHeaderDate = (TextView) row.findViewById(R.id.tv_exchange_row_header_date);
			row.setTag(holder);
		}

		FrameLayout headerFrame = (FrameLayout) row.findViewById(R.id.exchange_row_header);
		if (position == 0 || !dateFormatter.format(getItem(position - 1).getDate()).equals(dateFormatter.format(exchange.getDate()))) {
			if (headerFrame.getChildCount() == 0) {
				headerFrame.addView(layoutInflater.inflate(R.layout.listview_exchange_row_header, parent, false));
				holder.tvHeaderDate = (TextView) row.findViewById(R.id.tv_exchange_row_header_date);
			}
		} else {
			headerFrame.removeAllViews();
			holder.tvHeaderDate = null;
		}
		if(holder.tvHeaderDate != null) {
			holder.tvHeaderDate.setText(dateFormatter.format(exchange.getDate()));
		}
		holder.ivSrcImage.setImageResource(sCurrencyImages.get(exchange.getSource().getCurrency().getCode()));
		holder.tvSrcName.setText(exchange.getSource().getCurrency().getCode());
		holder.ivDstImage.setImageResource(sCurrencyImages.get(exchange.getDestination().getCurrency().getCode()));
		holder.tvDstName.setText(exchange.getDestination().getCurrency().getCode());
		holder.tvRate.setText(Repository.getDecimalFormat().format(exchange.getRate()));
		holder.tvValue.setText(Repository.getDecimalFormat().format(exchange.getValue()) + exchange.getSource().getCurrency().getSymbol());
		return row;
	}

	/**
	 * Реализация класса ViewHolder, хранящего ссылки на виджеты.
	 */
	public static class ViewHolder {
		public TextView tvSrcName;
		public ImageView ivSrcImage;
		public TextView tvDstName;
		public ImageView ivDstImage;
		public TextView tvRate;
		public TextView tvValue;
		public TextView tvHeaderDate;
	}
}
