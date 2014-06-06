package dk.slott.super_volley.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import dk.slott.super_volley.cache.BitmapLruImageCache;

/**
 * Implementation of volley's ImageCache interface. This manager tracks the application image loader and cache.
 * @author Trey Robinson
 *
 */
public class ImageCacheManager{
	private static ImageCacheManager mInstance;
	
	/**
	 * Volley image loader 
	 */
	private ImageLoader mImageLoader;

	/**
	 * Image cache implementation
	 */
	private BitmapLruImageCache mImageCache;
	
	/**
	 * @return
	 * 		instance of the cache manager
	 */
	public static ImageCacheManager getInstance(){
		if(mInstance == null)
			mInstance = new ImageCacheManager();
		
		return mInstance;
	}
	
	/**
	 * Initializer for the manager. Must be called prior to use. 
	 * 
	 * @param context
	 * 			application context
	 * @param uniqueName
	 * 			name for the cache location
	 * @param cacheSize
	 * 			max size for the cache
	 * @param compressFormat
	 * 			file type compression format.
	 * @param quality
	 */
	public void init(Context context, String uniqueName, int cacheSize, CompressFormat compressFormat, int quality){
		mImageCache = new BitmapLruImageCache(cacheSize);
		mImageLoader = new ImageLoader(RequestManager.getRequestQueue(), new BitmapLruImageCache(cacheSize));
	}
	
	public void removeBitmap(String url) {
		mImageCache.remove(url);
	}
	
	public Bitmap getBitmap(String url) {
		try {
			return mImageCache.getBitmap(createKey(url));
		} catch (NullPointerException e) {
			throw new IllegalStateException("Cache Not initialized");
		}
	}

	public void putBitmap(String url, Bitmap bitmap) {
		try {
			mImageCache.putBitmap(createKey(url), bitmap);
		} catch (NullPointerException e) {
			throw new IllegalStateException("Cache Not initialized");
		}
	}
	
	
	/**
	 * 	Executes and image load
	 * @param url
	 * 		location of image
	 * @param listener
	 * 		Listener for completion
	 */
	public void getImage(String url, ImageListener listener){
		mImageLoader.get(url, listener);
	}

	/**
	 * @return
	 * 		instance of the image loader
	 */
	public ImageLoader getImageLoader() {
		return mImageLoader;
	}
	
	/**
	 * Creates a unique cache key based on a url value
	 * @param url
	 * 		url to be used in key creation
	 * @return
	 * 		cache key value
	 */
	private String createKey(String url){
		return String.valueOf(url.hashCode());
	}
	
	
}
