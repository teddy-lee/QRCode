package com.dm.zbar.android.scanner;

import com.dm.zbar.android.examples.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

public class ZBarScannerActivity extends Activity implements
		Camera.PreviewCallback, ZBarConstants {

	private static final String TAG = "ZBarScannerActivity";
	private CameraPreview mPreview;
	private Camera mCamera;
	private ImageScanner mScanner;
	private Handler mAutoFocusHandler;
	private boolean mPreviewing = true;

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

		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);

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
			SymbolSet syms = mScanner.getResults();
			for (Symbol sym : syms) {
				String symData = sym.getData();
				if (!TextUtils.isEmpty(symData)) {
					// Intent dataIntent = new Intent();
					// dataIntent.putExtra(SCAN_RESULT, symData);
					// dataIntent.putExtra(SCAN_RESULT_TYPE, sym.getType());
					// setResult(Activity.RESULT_OK, dataIntent);
					// finish();
					Toast.makeText(this, "Scan Result = " + symData,
							Toast.LENGTH_SHORT).show();
					stopScan();
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
		}
		return true;
	}

	// Mimic continuous auto-focusing
	Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};
}
