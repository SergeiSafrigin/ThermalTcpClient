package thermapp.sdk.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import thermapp.sdk.ThermAppAPI;
import thermapp.sdk.ThermAppAPI_Callback;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends Activity implements ThermAppAPI_Callback {
    //creating a tag name for logs
    private static final String TAG = "MainActivity";
    //app context
    private Context context;
    //udp socket
    private Socket socket;
    //ip address of the udp server
    private String ipAddress;
    //port address of the udp server
    private Integer port;
    //true if the ip address and the port were set up in the preferences
    private boolean isServerDataSetUp;
    //is temperatures array size sent
    private boolean isTemperaturesSizeSent;
    //setting button
    private ImageButton settingsButton;

    //thermal camera sdk
    private ThermAppAPI mDeviceSdk = null;
	private RelativeLayout Splash_Lay;
	private RelativeLayout NoCam_Lay;

    //tcp server writer
    private OutputStream serverWriter;

    //width and height of the frame sent via tcp socket
    private int[] frameSize;

    //data used in order to make the thermal camera mode, black and white
    private int[] gray_palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate called");

        init(this);
    }

    /**
     * initiate everything
     */
    private void init(Context context) {
        //set app context
        this.context = context;

        Splash_Lay = (RelativeLayout) findViewById(R.id.Rel_Splash);
        Splash_Lay.setVisibility(View.VISIBLE);
        NoCam_Lay = (RelativeLayout) findViewById(R.id.Rel_Nocam);

        //get setting button from layout
        settingsButton = (ImageButton) findViewById(R.id.setting_btn);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "settings button clicked");
                //open preferences
                openPreference();
            }
        });

        //init gray palette in order to get black and white image from the camera
        gray_palette = new int[256];
        for (int i = 0; i < 256; i++)
            gray_palette[i] = 0xFF000000 | (i << 0) | (i << 8) | (i << 16);

        //if successfully initiated thermal camera sdk, launch the welcome activity
        if (InitSdk()) {
            final Intent i = new Intent(this, WelcomeActivity.class);
            i.putExtra("serialnum", Integer.toString(mDeviceSdk.GetSerialNumber()));
            startActivityForResult(i, 1);
        } else {
            NoCam_Lay.setVisibility(View.VISIBLE);
        }

        //listen to usb device de-attach events
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    /**
     * open preferences activity
     */
    private void openPreference() {
        Log.i(TAG, "opening preferences activity");

        Intent intent;
        //for older versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            intent = new Intent(this, Preferences.class);
        else //for newer versions than honeycomb
            intent = new Intent(this, PrefencesForNewVersions.class);

        startActivity(intent);
    }

    // Define USB detached event receiver
    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "received new usb device detached event");
            Log.d(TAG, intent.getAction());

            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.i(TAG, "usb device was detached, therefore closing the app");
                CloseApp();
            }
        }
    };

    /**
     * initiate the thermal camera sdk
     * @return
     */
	private boolean InitSdk() {
		// Create Developer SDK Instance
		mDeviceSdk = new ThermAppAPI(this);

		// Try to open usb interface
		try {
			mDeviceSdk.ConnectToDevice();
		} catch (Exception e) {

			// Close SDK
			mDeviceSdk = null;
			return false;
		}
		return true;
	}

    /**
     * set the camera mode to black and white
     */
	private void SetThermalMode_BW() {
        Log.i(TAG, "setting the thermal camera mode to black and white");

        if (mDeviceSdk != null) {
            try {
                mDeviceSdk.SetMode(1, gray_palette);
            } catch (Exception e) {
                Log.e(TAG, "failed to set the mode of the thermal camera app to black and white");
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "thermal camera sdk instance was nul!!");
        }
	}

    /**
     * on result from the welcome activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult event");

		if (resultCode == RESULT_OK) {
            Log.i(TAG, "activity for result returned RESULT_OK");

            String result = data.getStringExtra("result");

            if (result.equals("OK")) {
                Log.i(TAG, "the result received from the intent was \"OK\"");

                try {
                    Log.i(TAG, "trying to start the video of the thermal camera");
                    mDeviceSdk.StartVideo();
                } catch (Exception e) {
                    Log.e(TAG, "failed to start the video of the thermal camera");
                    Log.e(TAG, e.toString());

                    // Report error to use
                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                    dlgAlert.setMessage("Unable to start video: " + e.getMessage());
                    dlgAlert.setTitle("ThermApp");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }

                Splash_Lay.setVisibility(View.GONE);
            } else if (result.equals("EXIT")) {
                Log.i(TAG, "the result received from the intent was not \"OK\"");

                CloseApp();
            }

        } else if (resultCode == RESULT_CANCELED) { //bad result
            Log.i(TAG, "activity for result returned RESULT_CANCELED");
			CloseApp();
		}
	}

    /**
     * some weird voodoo
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "new intent received");

        if (Splash_Lay.getVisibility() == View.VISIBLE) {
            onCreate(new Bundle());
        }
        super.onNewIntent(intent);
    }

    /**
     * on new frame event from the thermal camera app
     * @param bmp
     */
    @Override public void OnFrameGetThermAppBMP(Bitmap bmp) {}

    /**
     * on new temperature event from the thermal camera
     * @param frame
     * @param width
     * @param height
     */
    @Override
    public void OnFrameGetThermAppTemperatures(int[] frame, int width, int height) {
            //if didn't sent the size of the frame array yet, send it first
            if (!isTemperaturesSizeSent) {
                //init frame size
                frameSize = new int[2];
                frameSize[0] = width;
                frameSize[1] = height;

                try {
                    //send frame size to the tcp server
                    serverWriter.write(int2byte(frameSize));
                } catch (IOException e) {
                    Log.e(TAG, "failed to send frame size to the server");
                    e.printStackTrace();
                }

                //send the width and the height
                isTemperaturesSizeSent = true;
            }

            try {
                //send frame to the tcp server
                serverWriter.write(int2byte(frame));
            } catch (IOException e) {
                Log.e(TAG, "failed to send frame to the server");
                e.printStackTrace();
            }
    }

    /**
     * update from shared preferences
     */
    private void updatePreferences() {
        //get preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        //get ip address from the preferences
        ipAddress = preferences.getString("ipAddress", null);
        //get port number from the preferences
        String portString = preferences.getString("port", null);
        port = portString != null ? Integer.parseInt(portString) : null;

        //check if the ip address and the port were actually filled
        if (!(isServerDataSetUp = ipAddress != null || port != null)) {
            Toast.makeText(context, "please set up the ip address and port number", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * convert int array into byte array
     * @param src
     * @return
     */
    private static byte[] int2byte(int[] src) {
        byte[] output = new byte[src.length*4];

        for(int i = 0; i < src.length; i++) {
            int position = i << 2;
            output[position | 0] = (byte)((src[i] >>  0) & 0xFF);
            output[position | 1] = (byte)((src[i] >>  8) & 0xFF);
            output[position | 2] = (byte)((src[i] >> 16) & 0xFF);
            output[position | 3] = (byte)((src[i] >> 24) & 0xFF);
        }

        return output;
    }

    /**
     * establish connection with the udp server
     */
    private void establishConnection(){
        Log.i(TAG, "opening a connection with the server");
        new Thread() {
            public void run() {
                try {
                    socket = new Socket(ipAddress, port);
                    //get server writer
                    serverWriter = socket.getOutputStream();
                    Log.i(TAG, "a connection with the server was successfully established");
                } catch (Exception e) {
                    Log.e(TAG, "failed to establish a connection with the server");
                    Log.e(TAG, e.toString());
                }
            }
        }.start();
    }

    /**
     * end the connection with the udp server
     */
    private void endConnection(){
        Log.i(TAG, "closing the connection with the udp server");
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "failed to close the socket to the server");
            e.printStackTrace();
        }
    }

    /**
     * on app resumed
     */
    @Override
    public void onResume() {
        Log.i(TAG, "onResume called");

        //set the thermal camera mode to black and white
        SetThermalMode_BW();

        super.onResume();

        //update user preferences (ip address and port number)
        updatePreferences();

        //establish a connection with the server if the ip address and port were set
        if (isServerDataSetUp)
            establishConnection();
    }

    @Override
    public void onPause() {
        super.onPause();

        //end the connection with the udp server
        endConnection();
    }

    /**
     * on application destroyed
     */
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy called");

        //close the app if was about to finish
        if (this.isFinishing())
            CloseApp();
    }

    /**
     * close the application by force
     */
    private void CloseApp() {
        Log.i(TAG, "closing the app");

        //close the app
        this.finish();
        System.exit(0);
    }
}