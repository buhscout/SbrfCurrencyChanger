package scout.sbrfcurrencychanger.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class WaitDialog extends DialogFragment {

	//private static final int TASK_PROGRESS = 1;
	private static final int TASK_MESSAGE = 2;
	private static final String DATA_KEY_MESSAGE = "message";
	private static final String DATA_KEY_ID = "id";
	private static final ThreadPoolExecutor sThreadPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	private static final Map<String, String> sTasks = new HashMap<>();
	private static WaitDialog sWaitDialog;

	@Override
	public void onSaveInstanceState(Bundle outState) {

	}

	@SafeVarargs
	public static <TParams, TProgress, TResult> void execute(Activity activity, WaitDialogTask<TParams, TProgress, TResult> task, TParams... args) {
		execute(activity, "", task, args);
	}

	@SafeVarargs
	private static <TParams, TProgress, TResult> void execute(Activity activity, String message, WaitDialogTask<TParams, TProgress, TResult> task, TParams... args) {
		Fragment fragment;
		final String tag = "WaitDialogFragment";

		sTasks.put(task.getId(), message);
		FragmentManager fragmentManager = activity.getFragmentManager();
		if (sWaitDialog != null && sWaitDialog.isInLayout()) {
			fragment = fragmentManager.findFragmentByTag(tag);
		} else {
			fragment = sWaitDialog;
		}
		if (fragment == null) {
			sWaitDialog = new WaitDialog();
			sWaitDialog.refreshMessage();
			sWaitDialog.setCancelable(false);
			sWaitDialog.show(fragmentManager.beginTransaction(), tag);
		} else {
			sWaitDialog = (WaitDialog) fragment;
			sWaitDialog.refreshMessage();
		}
		task.setHandler(sWaitDialog.mHandler);
		task.setOnTaskListener(new TaskListener(task));
		task.executeOnExecutor(sThreadPool, args);
	}

	private Handler mHandler;
	private ProgressDialog mProgressDialog;

	public WaitDialog() {
		mHandler = new MessageHandler();
	}

	private void refreshMessage() {
		String message = joinStrings("\r\n", sTasks.values());
		setMessage(message);
		if (mProgressDialog != null) {
			mProgressDialog.setMessage(message);
		}
	}

	private void setMessage(String message) {
		Bundle args;
		args = getArguments();
		if(args == null) {
			args = new Bundle();
			args.putString(DATA_KEY_MESSAGE, message);
			setArguments(args);
		} else {
			args.putString(DATA_KEY_MESSAGE, message);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public ProgressDialog onCreateDialog(Bundle savedInstanceState) {
		String message = getArguments().getString(DATA_KEY_MESSAGE);
		mProgressDialog = new ProgressDialog(getActivity());
		mProgressDialog.setMessage(message);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.show();
		return mProgressDialog;
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		sThreadPool.shutdownNow();
		super.onCancel(dialog);
	}

	private static String joinStrings(String separator, Collection<String> strings) {
		String fullString = "";
		for (String entry : strings) {
			if (!fullString.isEmpty()) {
				fullString += separator;
			}
			fullString += entry;
		}
		return fullString;
	}

	public interface OnTaskListener {
		void onFinished(Object data);
		void onCanceled();
	}

	private static class TaskListener implements OnTaskListener {
		private final WaitDialogTask mTask;

		public TaskListener(WaitDialogTask task) {
			mTask = task;
		}

		@Override
		public void onFinished(Object data) {
			closeFragment();
		}

		@Override
		public void onCanceled() {
			closeFragment();
		}

		private void closeFragment() {
			sTasks.remove(mTask.getId());
			if (sTasks.size() == 0) {
				sWaitDialog.dismiss();
				sWaitDialog = null;
				mTask.setHandler(null);
			} else {
				sWaitDialog.refreshMessage();
			}
		}
	}

	private static class MessageHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == TASK_MESSAGE) {
				Bundle data = msg.getData();
				String message = data.getString(DATA_KEY_MESSAGE);
				String id = data.getString(DATA_KEY_ID);
				if (id != null && message != null) {
					sTasks.put(id, message);
					sWaitDialog.refreshMessage();
				}
			}
		}
	}

	public static abstract class WaitDialogTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {
		private Handler mHandler;
		private OnTaskListener mOnTaskListener;
		private String mId = UUID.randomUUID().toString();

		public String getId() {
			return mId;
		}

		public void setOnTaskListener(OnTaskListener onTaskListener) {
			mOnTaskListener = onTaskListener;
		}

		protected void setHandler(Handler handler) {
			mHandler = handler;
		}

		/**
		 * Установить сообщение
		 * @param message Сообщение
		 */
		protected void setMessage(String message) {
			if (mHandler != null) {
				Bundle data = new Bundle();
				data.putString(DATA_KEY_MESSAGE, message);
				data.putString(DATA_KEY_ID, getId());
				Message msg = new Message();
				msg.setData(data);
				msg.what = TASK_MESSAGE;
				mHandler.sendMessage(msg);
			}
		}

        /**
         * Установить значение строки прогресса
         * //@param value Значение от 0 до 100
         */
		/*protected void setProgress(int value) {
			if (mHandler != null) {
				Message msg = new Message();
				msg.what = TASK_PROGRESS;
				msg.arg1 = value;
				mHandler.sendMessage(msg);
			}
		}*/

		@Override
		protected void onCancelled() {
			if (mOnTaskListener != null) {
				mOnTaskListener.onCanceled();
			}
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(TResult result) {
			if (mOnTaskListener != null) {
				mOnTaskListener.onFinished(result);
			}
			super.onPostExecute(result);
		}

	}

}
