package dk.slott.super_volley.managers;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import dk.slott.super_volley.MainApplication;
import dk.slott.super_volley.utils.FileUtils;

public class CacheManager {
	protected static final String TAG = CacheManager.class.getSimpleName();
	private final File cacheFile;
	private final int cacheTimeout;

	/**
	 * A cacheTimeout of -1 means the cache is disabled.
	 * @param cacheIdentifier
	 * @param cacheTimeout
	 */
	public CacheManager(final String cacheIdentifier, final int cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
		// MSH: Dont create cache file for non cacheable data.
		this.cacheFile = (cacheTimeout >= 0) ? new File(MainApplication.getContext().getCacheDir(), cacheIdentifier) : null;
		//this.cacheFile = new File(MainApplication.getContext().getCacheDir(), cacheIdentifier);
		//Log.d(TAG, "cacheFile: " + cacheFile.getAbsolutePath());
	}

	/**
	 * Write cache using a BG thread to avoid locking of the UI.
	 * @param response
	 */
	public void writeCache(final JSONArray response) {
		writeCache(response.toString());
	}
	public void writeCache(final JSONObject response) {
		writeCache(response.toString());
	}
	/**
	 * Write data to file on a seperate thread.
	 * TODO: If this is invoked from the REQUEST class we are
	 * already on a seperate thread and we can skip this all together.
	 * @param data
	 */
	public void writeCache(final String data) {
		// A timeout value of -1 means caching is disabled.
		if(isDisabled()) {
			Log.d(TAG, "Ignore cache write");
			return;
		}
		final Runnable r = new Runnable() {
			public void run() {
				Log.d(TAG, "Writing to cache file on BG thread...");
				FileUtils.writeTextFile(data, CacheManager.this.cacheFile);
				Log.d(TAG, "Done!");
			}
		};
		// MSH: Run on seperate thread.
		final Thread t = new Thread(r);
		t.start();
	}
	
	public String readCache() {
		return FileUtils.readTextFile(this.cacheFile);
	}

	/**
	 * A timeout value of -1 means caching is disabled.
	 * @return
	 */
    public boolean isDisabled() {
		return this.cacheTimeout == -1;
	}
	
	/**
	 * True if cache file exists.
	 * @return
	 */
	public boolean isCached() {
		// A timeout value of -1 means caching is disabled.
		if(isDisabled())
			return false;
		return this.cacheFile.exists();
	}

	/**
	 * True if cache timeout has expired.
	 * @return
	 */
	public boolean isExpired() {
		final long cacheAge = System.currentTimeMillis() - cacheFile.lastModified();
		return !(cacheAge < cacheTimeout*1000);
	}

	/**
	 * Deletes all files from cache directory.
	 */
	public static void cleanDir() {
		final File[] files = MainApplication.getContext().getCacheDir().listFiles();
		for (File file : files)
			file.delete();

	}
}
