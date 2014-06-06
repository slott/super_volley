package dk.slott.super_volley;
/*
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.PoolingClientConnectionManager;
*/
import java.io.File;
import dk.slott.super_volley.config.Config;
import dk.slott.super_volley.config.Config.AuthMethod;
import dk.slott.super_volley.config.Config.BodyContentType;
import dk.slott.super_volley.managers.ImageCacheManager;
import dk.slott.super_volley.managers.RequestManager;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

public class MainApplication extends Application {
	private static final String TAG = MainApplication.class.getSimpleName();
	private static MainApplication instance;
	private static Toast toast;
	private static CompressFormat MEM_IMAGECACHE_COMPRESS_FORMAT = CompressFormat.PNG;
	private static int MEM_IMAGECACHE_QUALITY = 100;  //PNG is lossless so quality is ignored but must be provided
	private static int MEM_CACHE_SIZE = 100; // Number of "screens" to cache.
	public static File FILE_REQUEST_PATH; // Path where file request stores files.
//	public static Activity currentActivity = null;

	public MainApplication() {
		instance = this;
	}

	/**
	 * Address to prefix all REST calls.
	 * @param serverAddress
	 */
	protected void setServerAddress(final String serverAddress) {
		Config.SERVER_ADDRESS = serverAddress;
	}

	/**
	 * Default pattern is "&key=value" but smart REST servers works with "/key/value"
	 * @param queryPattern
	 */
	protected void setQueryPattern(final String queryPattern) {
		Config.QUERY_PATTERN = queryPattern;
	}

	/**
	 * Default pattern is "%s/%s/%s/format/json%s" ie. {server}/{area}/{function}/format/json{parameters}
	 * @param queryPattern
	 */
	protected void setUrlPattern(final String urlPattern) {
		Config.URL_PATTERN = urlPattern;
	}

	/**
	 * Googles unique number to track app usage.
	 * @param uaNumber
	 */
	protected void setUaNumber(final String uaNumber) {
		Config.UA_NUMBER = uaNumber;
	}

	/**
	 * Default timeout when making requests.
	 * @param timeout
	 */
	protected void setDefaultTimeoutMs(final int timeout) {
		Config.DEFAULT_TIMEOUT_MS = timeout;
	}

	/**
	 * Set custom date format for parsing JSON date values.
	 * @param format
	 */
	protected void setDateFormat(final String format) {
		Config.DATE_FORMAT = format;
	}

	/**
	 * Change request body content type to something else like application/json.
	 * @param bodyContentType
	 */
	protected void setBodyContentType(final BodyContentType bodyContentType) {
		Config.REQUEST_BODY_CONTENT_TYPE = bodyContentType;
	}

	/**
	 * Change default request auth method (POST or HTTP_BASIC_AUTH)
	 * @param authMethod
	 */
	protected void setAuthMethod(final AuthMethod authMethod) {
		Config.AUTH_METHOD = authMethod;
	}

	private void createImageCache() {
		final int mem_imagecache_size = getScreenSize() * MEM_CACHE_SIZE;
		Log.d(TAG, "mem_imagecache_size: " + mem_imagecache_size);
		ImageCacheManager.getInstance().init(this, this.getPackageCodePath(), mem_imagecache_size, MEM_IMAGECACHE_COMPRESS_FORMAT, MEM_IMAGECACHE_QUALITY);
	}

	/**
	 * Get the screen pixel count
	 * @return
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private int getScreenSize() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		final int width;
		final int height;
		final Point size = new Point();
		if (Build.VERSION.SDK_INT >= 13) {
			display.getSize(size);
			width = size.x;
			height = size.y;
		}
		else {
			width = display.getWidth();
			height = display.getHeight();
		}
		return height*width;
	}

/*
Call this from onCreate/onPause
	// TODO: Use weak reference !
	public synchronized static void setCurrentActivity(final Activity activity) {
		Log.d(TAG, "setCurrentActivity: " + activity.getTitle());
		currentActivity = activity;
	}
	
	public synchronized static Activity getCurrentActivity() {
		Log.d(TAG, "getCurrentActivity: " + currentActivity.getTitle());
		return currentActivity;
	}
*/	
	/**
	 * MSH: Triggered by the main project if extended.
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		FILE_REQUEST_PATH = getFilesDir();

/*
		// http://ogrelab.ikratko.com/using-volley-android-with-external-httpclient-4-2-x/ - cookie support
		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
		PoolingClientConnectionManager p = new PoolingClientConnectionManager(schemeRegistry, 65, java.util.concurrent.TimeUnit.SECONDS);
		RequestManager.init(this, new ExtHttpClientStack(new DefaultHttpClient(p)));
*/
		//RequestManager.init(this, new HurlStack());
		
		
		// This fails terrible because of missing connection manager...
//		RequestManager.init(this, new ApacheStack());

		
		// MSH: If a different stack is needed simply call init from your own sub class.
		RequestManager.init(this);
		
		createImageCache();
		super.onCreate();
	}

	/**
	 * Gets the application context.
	 * 
	 * @return the application context
	 */
	public static Context getContext() {
		return instance;
	}

	/**
	 * Display toast message and cancel any other toast message.
	 * @param message
	 */
	public static synchronized void displayToast(int message) {
		if(toast != null)
			toast.cancel();
		toast = Toast.makeText(instance, message, Toast.LENGTH_SHORT);
		toast.show();
	}

	/**
	 * Return version from manifest.
	 * @return
	 */
	public static String getVersionName() {
		try {
			PackageManager manager = getContext().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getContext().getPackageName(), 0);
			return info.versionName;
		}
		catch (Exception e) {
			Log.e(TAG, "Error getting version name");
		}
		return "";
	}

	/**
	 * Return version from manifest.
	 * @return
	 */
	public static int getVersionCode() {
		try {
			PackageManager manager = getContext().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getContext().getPackageName(), 0);
			return info.versionCode;
		}
		catch (Exception e) {
			Log.e(TAG, "Error getting version code");
		}
		return 0;
	}

}
