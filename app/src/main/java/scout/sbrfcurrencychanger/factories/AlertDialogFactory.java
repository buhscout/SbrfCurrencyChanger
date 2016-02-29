package scout.sbrfcurrencychanger.factories;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Фабрика диалоговых окон
 */
public class AlertDialogFactory {

	public static final int TYPE_ERROR_OK = 1;

	public static void show(Context context, int type) {
		show(context, type, null, null);
	}

	public static void show(Context context, int type, String message) {
		show(context, type, message, null);
	}

	public static void show(Context context, int type, String message, String title) {
		get(context, type, message, title).show();
	}

	public static AlertDialog get(Context context, int type) {
		return get(context, type, null, null);
	}

	public static AlertDialog get(Context context, int type, String message) {
		return get(context, type, message, null);
	}

	public static AlertDialog get(Context context, int type, String message, String title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		switch (type) {
			case TYPE_ERROR_OK:
				if (title == null) {
					title = "Ошибка!";
				}
				builder.setTitle(title);
				builder.setMessage(message);
				builder.setCancelable(true);
				builder.setNegativeButton("ОК", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				});
				return builder.create();
		}
		return builder.create();
	}

}

