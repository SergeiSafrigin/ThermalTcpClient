/*
 * FTPDownload connected to WCFService
 * Downlading bin files
 */

package thermapp.sdk.sample;

import thermapp.sdk.DeviceData;
import thermapp.sdk.DeviceData_Callback;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Welcome Activity Object.
 * 
 * <P>
 * Initial activity that takes care of:
 * <ul>
 * <li>Terms of use</li>
 * <li>Internet connection</li>
 * <li>Downloading of calibration tables</li>
 * </ul>
 */
public class WelcomeActivity extends Activity implements DeviceData_Callback {

	private ProgressBar mProgressBar;
	private TextView mProgressText;
	private TextView mSerial;
	private String serialnum = null;
	private LinearLayout mDownloadLayout;
	private LinearLayout mRetryLayout;
	private LinearLayout mTermsLayout;
	private Button mRetryButton;
	private SharedPreferences mPrefs;
	private Button mTermsButton;
	private Button mNotAcceptButton;
	private ImageButton mAcceptButton;

	private DeviceData mDeviceData;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Get app preferences
		mPrefs = PreferenceManager
				.getDefaultSharedPreferences(WelcomeActivity.this);

		// Get controls instances
		mDownloadLayout  = (LinearLayout) findViewById(R.id.donwload_lay);
		mRetryLayout     = (LinearLayout) findViewById(R.id.retry_lay);
		mTermsLayout     = (LinearLayout) findViewById(R.id.terms_lay);
		mRetryButton     = (Button) findViewById(R.id.retry_button);
		mTermsButton     = (Button) findViewById(R.id.button_tofu);
		mAcceptButton    = (ImageButton) findViewById(R.id.imageButton_accept);
		mNotAcceptButton = (Button) findViewById(R.id.button_not_accept);

		// Setup buttons listeners
		mTermsButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mTermsButton));
		mAcceptButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mAcceptButton));
		mNotAcceptButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mNotAcceptButton));

		mTermsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent in = new Intent(WelcomeActivity.this, TermsOfUse.class);
				startActivity(in);

			}
		});

		mAcceptButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// Save confirmation
				mPrefs.edit().putBoolean("terms_shown", true).commit();

				// Check device data
				checkDeviceData();
			}
		});

		mNotAcceptButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent returnIntent = new Intent();
				returnIntent.putExtra("result", "EXIT");
				setResult(RESULT_OK, returnIntent);
				finish();

			}
		});

		mRetryButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				checkDeviceData();
			}
		});

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar_download);
		mProgressText = (TextView) findViewById(R.id.textView_perc);
		
		// Get serial number from main activity
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			serialnum = extras.getString("serialnum");
		}
		
		// Display serial number
		mSerial = (TextView) findViewById(R.id.textView_serial);
		mSerial.setText("Serial #: " + serialnum);

		// Create instance for ThermApp device data
		mDeviceData = new DeviceData(serialnum);
		
		// Check if we need to confirm terms of use  
		if (!mPrefs.getBoolean("terms_shown", false)) {
			mDownloadLayout.setVisibility(View.GONE);
			mRetryLayout.setVisibility(View.GONE);
			return;
		}

		// If all goes well, check if we have the device data locally
		checkDeviceData();
	}

	protected void checkDeviceData() {
		
		// Hide terms
		mTermsLayout.setVisibility(View.GONE);
		mRetryLayout.setVisibility(View.GONE);
		
		// Show download layout
		mDownloadLayout.setVisibility(View.VISIBLE);

		// Do we have everything we need?
		if(mDeviceData.IsDataAvialable()) {
			OnDownloadFinished();
			return;
		}

		// handle network disconnected (show retry)
		if (!isNetworkConnected()) {
			mRetryLayout.setVisibility(View.VISIBLE);
			mDownloadLayout.setVisibility(View.GONE);
			return;
		}

		// Else - start downloading
		mDeviceData.StartDownload(this);
	}

	public boolean isNetworkConnected() {
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getNetworkInfo(0);

			if (netInfo != null
					&& netInfo.getState() == NetworkInfo.State.CONNECTED) // 3G
			{
				return true;
			}

			else {
				netInfo = cm.getNetworkInfo(1);
				if (netInfo != null
						&& netInfo.getState() == NetworkInfo.State.CONNECTED
						&& WifiSignal() >= 30)
					return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	private int WifiSignal() {

		WifiManager wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wfm.getConnectionInfo();
		return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 100);
	}

	@Override
	public void OnDownloadFinished() {

		Intent returnIntent = new Intent();
		returnIntent.putExtra("result", "OK");
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	@Override
	public void OnError(String errorMsg) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				WelcomeActivity.this);
		builder.setTitle("ThermApp").setMessage(errorMsg).setCancelable(false)
				.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						System.exit(0);
					}
				});

		AlertDialog alert = builder.create();
		if (!isFinishing())
			alert.show();
	}

	@Override
	public void OnUpdateProgress(int value) {
		mProgressBar.setProgress(value);
		mProgressText.setText("" + value + "%");
	}
}
