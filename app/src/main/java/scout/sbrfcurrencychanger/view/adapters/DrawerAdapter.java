package scout.sbrfcurrencychanger.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.view.NavigationItem;

public class DrawerAdapter extends ArrayAdapter<NavigationItem> {

	private List<Integer> mCheckedItems;
	private boolean mCheckedIsSingle;

	public DrawerAdapter(Context context) {
		super(context, 0);
		mCheckedItems = new ArrayList<>();
	}

	public void addItem(NavigationItem item) {
		add(item);
	}

	public void setCheckedIsSingle(boolean checkedIsSingle) {
		mCheckedIsSingle = checkedIsSingle;
	}

	public void invertCheckState(Integer position) {
		NavigationItem item = getItem(position);
		if (item == null || item.isHeader()) {
			return;
		}
		if (mCheckedItems.contains(position)) {
			mCheckedItems.remove(position);
		} else {
			if (mCheckedIsSingle) {
				mCheckedItems.clear();
			}
			mCheckedItems.add(position);
		}
		notifyDataSetInvalidated();
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).isHeader() ? 0 : 1;
	}

	@Override
	public boolean isEnabled(int position) {
		return !getItem(position).isHeader();
	}

	public static class ViewHolder {
		public final ImageView Icon;
		public final TextView Title;
		public final TextView Counter;

		public ViewHolder(TextView title, TextView counter, ImageView icon) {
			Title = title;
			Counter = counter;
			Icon = icon;
		}
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		NavigationItem item = getItem(position);
		ViewHolder holder = null;
		if (convertView != null) {
			Object tag = convertView.getTag();
			if (tag instanceof ViewHolder) {
				holder = (ViewHolder) tag;
			}
		}
		if (holder == null) {
			TextView tvTitle;
			TextView txCounter = null;
			ImageView ivIcon = null;

			int layout = item.isHeader() ? R.layout.navigation_header : R.layout.navigation_item;
			convertView = LayoutInflater.from(getContext()).inflate(layout, null);

			if (item.isHeader()) {
				tvTitle = (TextView) convertView.findViewById(R.id.navigation_header_title_tvTitle);
			} else {
				tvTitle = (TextView) convertView.findViewById(R.id.navigation_item_counter_tvTitle);
				txCounter = (TextView) convertView.findViewById(R.id.navigation_item_counter_tvCounter);
				ivIcon = (ImageView) convertView.findViewById(R.id.navigation_item_counter_ivIcon);
			}
			holder = new ViewHolder(tvTitle, txCounter, ivIcon);
			convertView.setTag(holder);
		}
		if (holder.Title != null) {
			holder.Title.setText(item.getTitle());
		}
		if (holder.Counter != null) {
			if (item.getCounter() > 0) {
				holder.Counter.setVisibility(View.VISIBLE);
				//int counter = ((MainActivity) getContext()).getNewHistoryCounter();
				//if (counter > 0) {
				//	holder.Counter.setText(String.valueOf(counter));
				//} else {
				//	holder.Counter.setVisibility(View.GONE);
				//}
			} else {
				holder.Counter.setVisibility(View.GONE);
			}
		}
		if (holder.Icon != null) {
			if (item.getIcon() != 0) {
				holder.Icon.setVisibility(View.VISIBLE);
				holder.Icon.setImageResource(item.getIcon());
			} else {
				holder.Icon.setVisibility(View.GONE);
			}
		}
		if (mCheckedItems.contains(position)) {
			convertView.setBackgroundColor(getContext().getResources().getColor(R.color.blue_light));
		} else if (item.getColor() != null) {
			convertView.setBackgroundColor(item.getColor());
		} else if (!item.isHeader()) {
			convertView.setBackgroundColor(getContext().getResources().getColor(R.color.transparent));
		}
		return convertView;
	}

}
