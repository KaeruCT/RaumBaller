package jgame.platform;
import android.app.*;
import android.os.Bundle;
import android.content.res.AssetManager;

import android.hardware.*;
import android.view.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;

import android.content.Intent;
import android.content.DialogInterface;
import android.net.Uri;

import java.util.*;


public class JGActivity extends Activity {

	JGEngine eng;

	class UrlInvoker implements Runnable {
		String url;
		public UrlInvoker(String url) {
			this.url = url;
		}
		public void run() {
			// XXX currently resets the application state
			Intent i = new Intent(Intent.ACTION_VIEW);
			//i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.setData(Uri.parse(url));
			startActivity(i);
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		eng = JGEngine.current_engine;
		eng.initActivity(this);
	}

	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		eng.start();
		eng.sensormanager.registerListener(eng.canvas, eng.accelerometer,
			SensorManager.SENSOR_DELAY_FASTEST);

	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
		eng.stop();
		eng.sensormanager.unregisterListener(eng.canvas);
	}

	public static final int SOUNDDIALOG=0;
	public static final int ACCELDIALOG=1;

	protected Dialog onCreateDialog(int id) {
		//if (eng==null || !(eng instanceof StdGame)) return null;
		//if (!((StdGame)eng).audio_dialog_at_startup) return null;
		if (id == SOUNDDIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Enable sound?");
			//builder.setCancelable(false);
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (eng!=null && eng instanceof StdGame) {
						((StdGame)eng).audioenabled=true;
					}
				}
			});
			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (eng!=null && eng instanceof StdGame) {
						((StdGame)eng).audioenabled=false;
					}
					//dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		} else { // ACCELDIALOG
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Hold your phone in the desired 'zero' orientation and press OK.");
			//builder.setCancelable(false);
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (eng!=null && eng instanceof StdGame) {
						((StdGame)eng).accelzerovector = eng.getAccelVec();
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
	}

	static final int MAINGROUP = Menu.FIRST;

	static final int RESUME = Menu.FIRST;
	static final int QUITGAME   = Menu.FIRST+1;
	static final int SETACCEL   = Menu.FIRST+2;
	static final int SETTINGS   = Menu.FIRST+3;
	static final int SETTINGS_SUBMENU   = Menu.FIRST+4;

    private static final int REQUEST_CODE_PREFERENCES = 1;

	// standard menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, RESUME, Menu.NONE, "Resume");
		menu.add(Menu.NONE, QUITGAME, Menu.NONE, "Quit To Menu");
		// XXX StdGame specific
		if (eng instanceof StdGame && eng.hasAccelerometer()) {
			if ( ((StdGame)eng).accel_set_zero_menu) {
				menu.add(Menu.NONE, SETACCEL, Menu.NONE,
					"Calibrate accelerometer");
			}
		}
		if (eng.settings_array.size() > 0) {
			menu.add(Menu.NONE, SETTINGS, Menu.NONE, "Settings");
		}
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		eng.start();
		switch (item.getItemId()) {
			case RESUME:
				// nothing extra needs to be done
			break;
			case QUITGAME:
				// XXX StdGame specific
				// we should have a quit signal in the api or something
				if (eng instanceof StdGame) {
					((StdGame)eng).gameOver();
				}
				//finish();
			break;
			case SETACCEL:
				eng.canvas.post(new Runnable() {
					public void run() {
						showDialog(ACCELDIALOG);
					}
				});
			break;
			case SETTINGS:
				System.err.println("Settings menu");
				Intent launchPreferencesIntent = new Intent().setClass(this,
					JGPreferences.class);
				// Make it a subactivity so we know when it returns
				startActivityForResult(launchPreferencesIntent,
					REQUEST_CODE_PREFERENCES);
			break;
			//default:
			//if (item.getItemId() >= SETTINGS_SUBMENU) {
			//	Settings s = eng.settings.get(new Integer(id));
			//}
		}
		return true;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// The preferences returned if the request code is what we had given
		// earlier in startSubActivity
		if (requestCode == REQUEST_CODE_PREFERENCES) {
			// Read a sample value they have set
			//updateCounterText();
		}
	}

//    private void updateCounterText() {
//        // Since we're in the same package, we can use this context to get
//        // the default shared preferences
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        final int counter = sharedPref.getInt(AdvancedPreferences.KEY_MY_PREFERENCE, 0);
//        mCounterText.setText(getString(R.string.counter_value_is) + " " + counter);
//   }




	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		eng.stop();
		return true;
	}
	@Override
	public void onOptionsMenuClosed (Menu menu) {
		eng.start();
	}
}

