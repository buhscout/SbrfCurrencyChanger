package scout.sbrfcurrencychanger.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.view.adapters.CheckedListPreferenceAdapter;

/**
 * Настройка с чекбоксами
 */
public class CheckedListPreference extends DialogPreference {

	private ArrayAdapter mListAdapter;
	private OnDialogClosedListener mOnDialogClosedListener;
	private OnCheckedChangeListener mOnCheckedChangeListener;
	private CheckedChangeListener mCheckedChangeListener;
	private OnShowDialogPreferenceListener mOnShowDialogPreferenceListener;
	private ListView mListView;
	private ArrayList<Integer> mCheckedRows = new ArrayList<>();
	private static final String BUNDLE_CHECKED_ROWS = "checkedRows";

	public CheckedListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ArrayAdapter getListAdapter() {
		return mListAdapter;
	}

	public void setListAdapter(ArrayAdapter listAdapter) {
		mListAdapter = listAdapter;
	}

	public void setOnDialogClosedListener(OnDialogClosedListener onDialogClosedListener) {
		mOnDialogClosedListener = onDialogClosedListener;
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
		mOnCheckedChangeListener = onCheckedChangeListener;
	}

	public void setOnShowDialogListener(OnShowDialogPreferenceListener onShowDialogListener) {
		mOnShowDialogPreferenceListener = onShowDialogListener;
	}

	@Override
	public void showDialog(Bundle state) {
		if(mOnShowDialogPreferenceListener != null) {
			ShowDialogPreferenceEventArgs e = new ShowDialogPreferenceEventArgs();
			mOnShowDialogPreferenceListener.onShowDialog(this, e);
			if(e.isCancel()) {
				return;
			}
		}
		super.showDialog(state);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			mCheckedRows = ((Bundle)state).getIntegerArrayList(BUNDLE_CHECKED_ROWS);
			if(mListView != null) {
				Adapter adapter = mListView.getAdapter();
				if(adapter != null) {
					((CheckedListPreferenceAdapter)adapter).setCheckedRows(mCheckedRows);
				}
			}
		}
		super.onRestoreInstanceState(null);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable parcelable = super.onSaveInstanceState();
		Bundle bundle;
		if(parcelable instanceof Bundle) {
			bundle = (Bundle)parcelable;
		} else {
			bundle = new Bundle();
		}
		bundle.putIntegerArrayList(BUNDLE_CHECKED_ROWS, mCheckedRows);
		return bundle;
	}

	public List<Integer> getCheckedRows() {
		return mCheckedRows;
	}

	public void putCheckedRow(int rowIndex) {
		mCheckedRows.add(rowIndex);
	}

	public final void setRowCheckedState(int rowIndex, boolean isChecked) {
		int index = mCheckedRows.indexOf(rowIndex);
		if(isChecked && index == -1) {
			if(mListView != null) {
				((CheckedListPreferenceAdapter)mListView.getAdapter()).setCheckedState(rowIndex, true);
			} else {
				mCheckedRows.add(rowIndex);
			}
		} else {
			if (!isChecked && index != -1) {
				if (mListView != null) {
					((CheckedListPreferenceAdapter) mListView.getAdapter()).setCheckedState(rowIndex, false);
				} else {
					mCheckedRows.remove(index);
				}
			}
		}
	}

	@Override
	protected View onCreateDialogView() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.checked_list_preference, null);

		mListView = (ListView) view.findViewById(R.id.checkedListPreference_lvData);
		CheckedListPreferenceAdapter adapter = new CheckedListPreferenceAdapter(mListAdapter, mCheckedRows);

		adapter.setOnCheckedChangeListener(mCheckedChangeListener != null ? mCheckedChangeListener : (mCheckedChangeListener = new CheckedChangeListener(this)));
		mListView.setAdapter(adapter);

		return view;
	}

	@Override
	protected void onDialogClosed(boolean isSuccess) {
		super.onDialogClosed(isSuccess);
		if(mOnDialogClosedListener != null) {
			mOnDialogClosedListener.onDialogClosed(this, isSuccess);
		}
		// Return if change was cancelled
		if (!isSuccess) {
			return;
		}
		// Notify activity about changes (to update preference summary line)
		notifyChanged();
	}

	public interface OnDialogClosedListener {
		void onDialogClosed(CheckedListPreference preference, boolean isOk);
	}

	public interface OnCheckedChangeListener {
		void onCheckedChanged(CheckedListPreference preference, int index, boolean isChecked);
	}

	public interface OnShowDialogPreferenceListener {
		void onShowDialog(CheckedListPreference preference, ShowDialogPreferenceEventArgs e);
	}

	class ShowDialogPreferenceEventArgs {
		private boolean mIsCancel;

		public void setCancel(boolean isCancel) {
			mIsCancel = isCancel;
		}

		public boolean isCancel() {
			return mIsCancel;
		}

	}

	class CheckedChangeListener implements CheckedListPreferenceAdapter.OnCheckedChangeListener {

		CheckedListPreference mPreference;

		public CheckedChangeListener(CheckedListPreference preference) {
			mPreference = preference;
		}

		@Override
		public void onCheckedChanged(int index, boolean isChecked) {
			if (mOnCheckedChangeListener != null) {
				mOnCheckedChangeListener.onCheckedChanged(mPreference, index, isChecked);
			}
		}
	}

}
