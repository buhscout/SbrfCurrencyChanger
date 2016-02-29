package scout.sbrfcurrencychanger.view.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import scout.sbrfcurrencychanger.R;

/**
 * Адаптер для настройки с чекбоксами
 */
public class CheckedListPreferenceAdapter<T> extends ArrayAdapter<T> {

	private List<Integer> mCheckedRows = new ArrayList<>();
	private ArrayAdapter<T> mElementsAdapter;
	private OnCheckedChangeListener mOnCheckedChangeListener;

	public CheckedListPreferenceAdapter(ArrayAdapter<T> elementsAdapter, List<Integer> checkedRows) {
		super(elementsAdapter.getContext(), R.layout.checked_list_preference_row);
		mElementsAdapter = elementsAdapter;
		mCheckedRows = checkedRows;
		for (int i = 0; i < elementsAdapter.getCount(); i++) {
			add(elementsAdapter.getItem(i));
		}
	}

	public interface OnCheckedChangeListener {
		void onCheckedChanged(int index, boolean isChecked);
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
		mOnCheckedChangeListener = onCheckedChangeListener;
	}

	public void setCheckedRows(List<Integer> checkedRows) {
		mCheckedRows = checkedRows;
		notifyDataSetChanged();
	}

	public void setCheckedState(int rowIndex, boolean isChecked) {
		int index = mCheckedRows.indexOf(rowIndex);
		if(isChecked && index == -1) {
			if(mCheckedRows.add(rowIndex)) {
				if(mOnCheckedChangeListener != null) {
					mOnCheckedChangeListener.onCheckedChanged(rowIndex, true);
				}
				notifyDataSetChanged();
			}
		} else if(!isChecked && index != -1) {
			if(mCheckedRows.remove(Integer.valueOf(rowIndex))) {
				if(mOnCheckedChangeListener != null) {
					mOnCheckedChangeListener.onCheckedChanged(rowIndex, false);
				}
				notifyDataSetChanged();
			}
		}
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View row = convertView;
		DataHolder holder;
		if (row == null) {
			LayoutInflater inflater = ((Activity) parent.getContext()).getLayoutInflater();
			row = inflater.inflate(R.layout.checked_list_preference_row, parent, false);

			holder = new DataHolder();
			holder.Frame = (FrameLayout) row.findViewById(R.id.checked_list_preference_row_frame);
			holder.CheckBox = (CheckBox) row.findViewById(R.id.checked_list_preference_row_checkBox);
			row.setTag(holder);
		} else {
			holder = (DataHolder) row.getTag();
			holder.CheckBox.setOnCheckedChangeListener(null);
		}
		holder.CheckBox.setChecked(mCheckedRows != null && mCheckedRows.contains(position));
		holder.CheckBox.setOnCheckedChangeListener(new CheckedChangeListener(position));
		View elementView = null;
		if(holder.Frame.getChildCount() > 0) {
			elementView = holder.Frame.getChildAt(0);
			holder.Frame.removeView(elementView);
		}
		holder.Frame.addView(mElementsAdapter.getView(position, elementView, holder.Frame));
		return row;
	}

	class CheckedChangeListener implements CompoundButton.OnCheckedChangeListener {

		private int mPosition;

		public CheckedChangeListener(int position) {
			mPosition = position;
		}

		@Override
		public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
			setCheckedState(mPosition, b);
		}
	}

	public static class DataHolder {
		public FrameLayout Frame;
		public CheckBox CheckBox;
	}
}
