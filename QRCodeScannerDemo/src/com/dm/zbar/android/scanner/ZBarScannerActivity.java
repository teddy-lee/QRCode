package com.dm.zbar.android.scanner;

import java.io.IOException;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.dm.zbar.android.examples.R;

public class ZBarScannerActivity extends Activity implements
		Camera.PreviewCallback, ZBarConstants, View.OnClickListener {

	private static final String TAG = "ZBarScannerActivity";
	
	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;
	
	private CameraPreview mPreview;
	private Camera mCamera;
	private ImageScanner mScanner;
	private Handler mAutoFocusHandler;
	private boolean mPreviewing = true;
	private boolean playBeep = true;
	private MediaPlayer mediaPlayer;
	
	FrameLayout preview;

	static {
		System.loadLibrary("iconv");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isCameraAvailable()) {
			// Cancel request if there is no rear-facing camera.
			cancelRequest();
			return;
		}

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mAutoFocusHandler = new Handler();

		// Create a RelativeLayout container that will hold a SurfaceView,
		// and set it as the content of our activity.
		mPreview = new CameraPreview(this, this, autoFocusCB);
		setContentView(R.layout.main);

		preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.setOnClickListener(this);
		preview.addView(mPreview);
		
		// open Vibrator 
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		// init beep
		initBeepSound();

		// Create and configure the ImageScanner;
		setupScanner();
	}

	public void setupScanner() {
		mScanner = new ImageScanner();
		mScanner.setConfig(0, Config.X_DENSITY, 3);
		mScanner.setConfig(0, Config.Y_DENSITY, 3);

		/*int[] symbols = new int[] {Symbol.QRCODE, Symbol.I25, Symbol.ISBN10, Symbol.ISBN13};
		if (symbols != null) {
			mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
			for (int symbol : symbols) {
				mScanner.setConfig(symbol, Config.ENABLE, 1);
			}
		}*/
	}

	@Override
	protected void onResume() {
		super.onResume();
		startScan();
	}

	private void startScan() {
		// Open the default i.e. the first rear facing camera.
		mCamera = Camera.open();
		if (mCamera == null) {
			// Cancel request if mCamera is null.
			cancelRequest();
			return;
		}

		mPreview.setCamera(mCamera);
		mPreview.showSurfaceView();

		mPreviewing = true;
	}
	
	/**
	 * initialize sound
	 */
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);
			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	/**
	 * Play sound and vibrate
	 */
	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		// open vibrate
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_DURATION);
	}

	private void stopScan() {
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.cancelAutoFocus();
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();

			// According to Jason Kuang on
			// http://stackoverflow.com/questions/6519120/how-to-recover-camera-preview-from-sleep,
			// there might be surface recreation problems when the device goes
			// to sleep. So lets just hide it and
			// recreate on resume
			mPreview.hideSurfaceView();

			mPreviewing = false;
			mCamera = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopScan();
	}

	public boolean isCameraAvailable() {
		PackageManager pm = getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public void cancelRequest() {
		Intent dataIntent = new Intent();
		dataIntent.putExtra(ERROR_INFO, "Camera unavailable");
		setResult(Activity.RESULT_CANCELED, dataIntent);
		finish();
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		Camera.Size size = parameters.getPreviewSize();

		Image barcode = new Image(size.width, size.height, "Y800");
		barcode.setData(data);

		int result = mScanner.scanImage(barcode);

		if (result != 0) {
			mCamera.cancelAutoFocus();
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mPreviewing = false;
			playBeepSoundAndVibrate();
			SymbolSet syms = mScanner.getResults();
			for (Symbol sym : syms) {
				String symData = sym.getData();
				if (!TextUtils.isEmpty(symData)) {
					Toast.makeText(this, "Scan Result = " + symData, Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}
	}

	private Runnable doAutoFocus = new Runnable() {
		public void run() {
			if (mCamera != null && mPreviewing) {
				mCamera.autoFocus(autoFocusCB);
			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (!mPreviewing) {
			startScan();
		} else {
			stopScan();
		}
		return true;
	}
	
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	// Mimic continuous auto-focusing
	Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.cameraPreview:
			if (!mPreviewing) {
				stopScan();
				startScan();
			}
			
			break;

		default:
			break;
		}
	}
}
