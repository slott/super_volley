package dk.slott.super_volley.managers;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;
import dk.slott.super_volley.MainApplication;
import dk.slott.super_volley.R;
import dk.slott.super_volley.config.Config;
import dk.slott.super_volley.models.ErrorModel;
import dk.slott.super_volley.models.JSonModel;
import dk.slott.super_volley.requests.FileRequest;
import dk.slott.super_volley.requests.GsonRequest;
import dk.slott.super_volley.requests.JsonArrayAuthRequest;
import dk.slott.super_volley.tasks.ProcessJSONResponseArrayTask;

/**
 * Data manager with tight integration to Volley.
 *
 * @author Morten Slott Hansen
 */
public class DataManagerHelper {
	private static final String TAG = DataManagerHelper.class.getSimpleName();
	public static final String REST_PREFIX = Config.SERVER_ADDRESS;
	protected static int DEFAULT_CACHE_TIMEOUT = 3600;
	protected static int DAY_CACHE_TIMEOUT = 86400;
	// Use cached data and right away request new data.
	protected static int CACHE_AND_REQUEST = 0;
	private WeakReference<Activity> activity;
	private int progressBarCount = 0;
	private final Gson gson;
	protected static String CACHE = "cache";
	final static Map<String, String> authParams = new HashMap<String, String>();

	/**
	 * We use the activity to show a progress spinner whenever there is network activity. This is also
	 * used to tag every request to the activity that spawn them so they can be terminated when an activity
	 * is paused.
	 *
	 * @param activity
	 */
	public DataManagerHelper(final Activity activity) {
		this.activity = new WeakReference<Activity>(activity);
		this.gson = createGsonBuilder(Config.DATE_FORMAT);
	}

	/**
	 * Create a gson builder with a more advanced date parser.
	 * http://danwiechert.blogspot.dk/2013/02/gson-and-date-formatting.html
	 *
	 * @param dateFormat
	 * @return
	 */
	private static Gson createGsonBuilder(final String dateFormat) {
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
			final DateFormat df = new SimpleDateFormat(dateFormat, Locale.US);

			@Override
			public Date deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
				try {
					return df.parse(json.getAsString());
				} catch (final java.text.ParseException e) {
					Log.e(TAG, "ParseException: " + e);
					return null;
				}
			}
		});
		return builder.create();
	}

	/**
	 * Show the spinning progressbar in title.
	 * MSH: Synchronized together with @hideProgressBar
	 */
	protected void showProgressBar() {
		synchronized (this) {
			if (this.progressBarCount == 0)
				setProgressBarIndeterminateVisibility(true);
			this.progressBarCount++;
			Log.d(TAG, "showProgressBar: " + this.progressBarCount);
		}
	}

	/**
	 * Hide the spinning progressbar in title.
	 * MSH: Synchronized together with @showProgressBar
	 */
	protected void hideProgressBar() {
		synchronized (this) {
			this.progressBarCount--;
			if (this.progressBarCount == 0)
				setProgressBarIndeterminateVisibility(false);
			Log.d(TAG, "hideProgressBar: " + this.progressBarCount);
		}
	}

	/**
	 * Show/Hide Progressbar (aka. spinner) in the action bar.
	 * Activity is null if datamanager is created without an activity ie. from inside a provioder with no UI.
	 *
	 * @param b
	 */
	private void setProgressBarIndeterminateVisibility(final boolean b) {
		final Activity activity = this.getActivity();
		if (activity != null) {
			Log.d(TAG, "class name: " + activity.getClass().getName());

			// MSH: Need this to determine if current activity is of type SherlockFragmentActivity.
			Class<?> sherlockFragmentActivity = null;
			try {
				sherlockFragmentActivity = Class.forName("com.actionbarsherlock.app.SherlockFragmentActivity");
			}
			catch (ClassNotFoundException e) {
				Log.d(TAG, "ClassNotFoundException: " + e);
			}

			// MSH: Take action based on activity type.
			if (sherlockFragmentActivity != null && sherlockFragmentActivity.isInstance(activity)) {
				Log.d(TAG, "We have a SherlockFragmentActivity!");
				// MSH: Trigger support method.
				try {
					Method method = activity.getClass().getMethod("setSupportProgressBarIndeterminateVisibility", boolean.class);
					method.invoke(activity, b);
				}
				catch (Exception e) {
					Log.e(TAG, "Exception: " + e);
				}
				Log.d(TAG, "Done calling setSupportProgressBarIndeterminateVisibility...");
			}
			else {
				Log.d(TAG, "We have a regular activity...");
				// MSH: Trigger regular method.
				activity.setProgressBarIndeterminateVisibility(b);
				Log.d(TAG, "Done calling setProgressBarIndeterminateVisibility...");
			}
		}
	}

	/**
	 * Factory method for returning a query map with a user defined template for converting the map to a string.
	 * @return QueryMap
	 */
	public static QueryMap getQueryMap() {
		return new QueryMap();
	}

	/**
	 * Factory method for returning a resource map.
	 * http://en.wikipedia.org/wiki/Representational_state_transfer#Applied_to_web_services
	 * @return resource map for storing url resources
	 * @see ResourceMap
	 */
	public static ResourceMap getResourceMap() {
		return new ResourceMap();
	}

	/**
	 * Base64 encode string.
	 *
	 * @param input
	 * @return
	 */
	protected static String base64Encode(final String input) {
		return Base64.encodeToString(input.getBytes(), Base64.NO_WRAP);
	}

	private void applyQueryTemplate(final QueryMap qm) {
		if (qm != null)
			qm.setQueryTemplate(Config.QUERY_PATTERN);
	}

	/**
	 * Extracts cache timeout from query string. Returns -1 if undefined.
	 *
	 * @param qm
	 * @return
	 */
	private int processCacheTimeout(final QueryMap qm) {
		if (qm != null) {
			// MSH: Look for cache timeout.
			if (qm.containsKey(CACHE)) {
				final int cache = (Integer) qm.get(CACHE);
				// MSH: Remove cache key so it is not sent to the server.
				qm.remove(CACHE);
				return cache;
			} else
				return -1; // Default is disabled
		} else
			return -1; // Default is disabled
	}

	/**
	 * Volley caches all request data and uses them when offline. However untill the server can correctly supply Cache-Control headers we still
	 * get a better performance if we handle some caching ourselves.
	 *
	 * @param url
	 * @param requestMethod
	 * @param resultListener
	 */
	protected void requestDataUsingCache(final Enum<?> area, final Enum<?> function, final int requestMethod, final QueryMap qm, final ResultListenerNG<JSONObject> resultListener) {
		applyQueryTemplate(qm);
		final int cacheTimeout = processCacheTimeout(qm);
		Log.d(TAG, "cacheTimeout: " + cacheTimeout);

		// MSH: Generate url based on Request Method.
		JSONObject jsonRequest = null;
		final String url;
		// MSH: If it is a post parameters must be sent as part of the requestBody - not regular url parameters.
		if (requestMethod == Request.Method.POST) {
			jsonRequest = mapToJson(qm);
			url = generateUrl(area, function);
		}
		// MSH: Else just append as a regular url query string.
		else {
			if (qm != null) {
				url = generateUrl(area, function, qm.toString());
			} else
				url = generateUrl(area, function);
		}

		Log.d(TAG, "requestData url: " + url);
		if (jsonRequest != null)
			Log.d(TAG, "requestData jsonRequest: " + jsonRequest.toString());

		final CacheManager cacheManager = new CacheManager(md5(url), cacheTimeout);

		// Lookup data in cache.
		if (cacheManager.isCached()) {
			Log.d(TAG, "Cache hit");
			// MSH: Fetch cache on BG thread.
			final Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						final JSONObject cachedJasonObject = new JSONObject(cacheManager.readCache());
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								// Show cached result regardless of expiration date. If expired a new request will be made and the data will get updarted once the server returns the data.
								resultListener.onSuccess(cachedJasonObject);
							}
						});
					} catch (JSONException e) {
						Log.e(TAG, "JSONException: " + e);
					}
				}
			};
			final Thread t = new Thread(r);
			t.start();

			// Has cache expired?
			if (cacheManager.isExpired()) {
				Log.d(TAG, "Cache has expired - request new data");
			} else {
				Log.d(TAG, "Cache is still valid - no need to request new data");
				return;
			}
		} else
			Log.d(TAG, "Cache miss");

		// MSH: Setup request for fetching data.
		final JsonObjectRequest jr = new JsonObjectRequest(requestMethod, url, jsonRequest,
			new Response.Listener<JSONObject>() {
				@Override
				public void onResponse(final JSONObject response) {
					hideProgressBar();
					// Cache response.
					cacheManager.writeCache(response);
					resultListener.onSuccess(response);
				}
			},
			new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					hideProgressBar();
					resultListener.onError(processVolleyError(error));
				}
			}
		);
		processRequest(jr);
	}

	/**
	 * Generic method for requesting JSON Object data.
	 * Note that requestMethod should be Request.Method.GET/POST/DELETE/PUT
	 * The post data is sent to the request class through the 3rd parameter as a JSON object.
	 * @param resultListener
	 */

	/**
	 * This is used for raw array without a name ie. JSONArray and not JSONObject.
	 *
	 * @param url
	 * @param resultListener
	 */
	public void requestData(final String url, final ResultListenerNG<JSONArray> resultListener) {
		Log.d(TAG, "Requesting json data from: " + url);

		final JsonArrayAuthRequest jr = new JsonArrayAuthRequest(url,
			new Response.Listener<JSONArray>() {
				@Override
				public void onResponse(JSONArray response) {
					hideProgressBar();
					resultListener.onSuccess(response);
				}
			},
			new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					hideProgressBar();
					resultListener.onError(processVolleyError(error));
				}
			}
		);
		processRequest(jr);
	}

	/**
	 * TODO: Make private as it should not be called directly.
	 * @param url
	 * @param resultListener
	 */
	protected void requestString(final String url, final ResultListenerNG<String> resultListener) {
		
	}
	
	/**
	 * Method for requesting JSON data and parse on background thread with resulting GSON objects.
	 *
	 * @param <T>
	 * @param url            REST URL
	 * @param modelClass     class which will be used to map the JSON response to objects of the specified class
	 * @param resultListener Callback which will be notified upon GSON parsing is done
	 */
	protected <T> void gsonGenericRequest(final int method, final String url, final JSONObject jsonRequest, final int cacheTimeout, final Class<T> clazz, final ResultListenerNG<T> resultListener) {
		final CacheManager cacheManager = new CacheManager(md5(url), cacheTimeout);
		// Lookup data in cache.
		if (cacheManager.isCached()) {
			Log.d(TAG, "Cache hit");
			// MSH: retrieve data from cache and parse into and object before returning.
			final String json = cacheManager.readCache();
			final Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						Log.d(TAG, "Parsing response into an object");
						final T result = gson.fromJson(json, clazz);
						// MSH: Return parsed result on the UI thread.
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								resultListener.onSuccess(result);
							}
						});
					} catch (Exception e) {
						Log.e(TAG, "Exception: " + e);
					}
				}
			};
			final Thread t = new Thread(r);
			t.start();

			// MSH: Cache is still valid - no need to request new data.
			if (cacheManager.isExpired())
				Log.d(TAG, "Cache has expired - request new data");
			else {
				Log.d(TAG, "Cache is still valid - no need to request new data");
				// Return and avoid sending a request to the server.
				return;
			}
		} else
			Log.d(TAG, "Cache miss");

		// MSH: Setup request object which will fetch data from the server.
		final GsonRequest<T> request = new GsonRequest<T>(method, url, jsonRequest, clazz, cacheTimeout,
			new Response.Listener<T>() {
				@Override
				public void onResponse(final T response) {
					hideProgressBar();
					// MSH: Run on bg thread to avoid hanging on the UI while converting.
					final Runnable r = new Runnable() {
						@Override
						public void run() {
							if (!cacheManager.isDisabled()) {
								//CLPET: https://code.google.com/p/google-gson/issues/detail?id=162
								Gson gson = new GsonBuilder().setDateFormat(Config.DATE_FORMAT).create();//CLPET: Threading issues...
								cacheManager.writeCache(gson.toJson(response));
							}
						}
					};
					final Thread t = new Thread(r);
					t.start();
					if(resultListener != null)
						resultListener.onSuccess(response);
				}
			},
			new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					hideProgressBar();
					if(resultListener != null)
						resultListener.onError(processVolleyError(error));
				}
			}
		);
		processRequest(request);
	}

	/**
	 * Tag request, schedule for execution and show progress bar.
	 * Note that the TTL is applies here which is then picked up by
	 * the HttpStack object ie. our custom @ExHttpClientStack
	 *
	 * @param r
	    */
	private void processRequest(final Request<?> r) {
		Log.d(TAG, "processRequest");
		
		final Activity activity = getActivity();
		if(activity != null) {
			// MSH: Tag request with current activity so that we may cancel all requests when leaving the activity.
			r.setTag(getActivity());
			Log.d(TAG, "Request tagged with activity: " + getActivity().getClass().getSimpleName());
		}
			Log.d(TAG, "Request not tagged!");

		// MSH: Inject Cache headers so etag gets processed in Network code
		if (r.getCacheEntry() == null)
			r.setCacheEntry(new Cache.Entry());

		// MSH: Change timeout policy. - http://stackoverflow.com/questions/17094718/android-volley-timeout
		if (DefaultRetryPolicy.DEFAULT_TIMEOUT_MS != Config.DEFAULT_TIMEOUT_MS)
			r.setRetryPolicy(new DefaultRetryPolicy(Config.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

		// Schedule request.
		RequestManager.getRequestQueue().add(r);
		showProgressBar();
	}

	/**
	 * Process VolleyError into a generic error model.
	 *
	 * @param error
	 * @return
	 */
	protected static ErrorModel processVolleyError(final VolleyError error) {
		// MSH: Avoid returning null.
		ErrorModel em = new ErrorModel();
		try {
			em = new ErrorModel(error);
		} catch (JSONException e) {
			Log.e(TAG, "JSONException: " + e);
			em = new ErrorModel();
			em.setErrorNumber(-1);
			em.setErrorMsg("Unknown error!!");
		}

		Log.d(TAG, "processVolleyError: " + em.getErrorNumber());

		// MSH: Always show a toast if there is a connection problem.
		if (em.getErrorNumber() == 0) {
			Log.d(TAG, "exception: " + error.getLocalizedMessage());
			Log.d(TAG, "msg: " + em.getErrorMsg());
			// MSH: Only show internet error if network related.
			final String message = error.getLocalizedMessage();
			if (message != null && message.indexOf("UnknownHostException") != -1)
				MainApplication.displayToast(R.string.no_internet_title);
		}
		return em;
	}

	/**
	 * MD5 value of a string.
	 *
	 * @param s
	 * @return
	 */
	private static synchronized String md5(final String string) {
		try {
			// Create MD5 Hash
			final MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(string.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			final StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException: " + e);
		}
		return "";
	}

	/**
	 * Convert Map into a generic JSON object.
	 *
	 * @param map
	 * @return
	 */
	protected static JSONObject mapToJson(final Map<String, Object> map) {
		final Gson gson = createGsonBuilder(Config.DATE_FORMAT);
		final String json = gson.toJson(map);
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			Log.e(TAG, "JSONException: " + e);
			return new JSONObject();
		}
	}

	/**
	 * Generate REST url. Api key and session id not included as they are sent as headers.
	 *
	 * @param area
	 * @param function
	 * @param queryString
	 * @return
	 */
	private static String generateUrl(final Enum<?> area, final Enum<?> function) {
		return generateUrl(area, null, function, "");
	}
	private static String generateUrl(final Enum<?> area, final Enum<?> function, final String queryString) {
		return generateUrl(area, null, function, queryString);
	}
	private static String generateUrl(final Enum<?> area, final ResourceMap rm, final Enum<?> function) {
		return generateUrl(area, rm, function, "");
	}
	protected static String generateUrl(final Enum<?> area, final Enum<?> function, final QueryMap qm) {
		return generateUrl(area, null, function, qm.toString());
	}
	protected static String generateUrl(final Enum<?> area, final ResourceMap rm, final Enum<?> function, final String queryString) {
		return String.format(Config.URL_PATTERN, REST_PREFIX, area.toString(), (rm != null) ? rm.toString() : "", (function != null) ? function.toString() : "", (queryString != null) ? queryString : "");
	}

	/**
	 * MSH: Process cache paramter before sending to @gsonGenericRequest with extracted cache timeout.
	 *
	 * @param area
	 * @param function
	 * @param qm
	 * @param clazz
	 * @param resultListener
	 */
	protected <T> void gsonGenericRequest(final Enum<?> area, final Enum<?> function, final QueryMap qm, final int requestMethod, final Class<T> clazz, final ResultListenerNG<T> resultListener) {
		gsonGenericRequest(area, null, function, qm, requestMethod, clazz, resultListener);
	}
	protected <T> void gsonGenericRequest(final Enum<?> area, final ResourceMap rm, final Enum<?> function, final QueryMap qm, final int requestMethod, final Class<T> clazz, final ResultListenerNG<T> resultListener) {
		applyQueryTemplate(qm);
		final int cacheTimeout = processCacheTimeout(qm);
		Log.d(TAG, "cacheTimeout: " + cacheTimeout);

		// MSH: Generate url based on Request Method.
		JSONObject jsonRequest = null;
		final String url;
		// MSH: If it is a post parameters must be sent as part of the requestBody - not regular url parameters.
		if (requestMethod == Request.Method.POST) {
			jsonRequest = mapToJson(qm);
			url = generateUrl(area, rm ,function);
		}
		// MSH: Else just append as a regular url query string.
		else {
			url = generateUrl(area, rm, function, (qm != null) ? qm.toString() : null);
		}
		Log.d(TAG, "gsonGenericRequest: " + url);

		// Add caching logic here and remove it from the gson request class.
		// Should be faster as we skip adding the request object to the queue...

		gsonGenericRequest(requestMethod, url, jsonRequest, cacheTimeout, clazz, resultListener);
	}

	/**
	 * Request a single JSON object.
	 *
	 * @param area
	 * @param function
	 * @param qm
	 * @param requestMethod
	 * @param resultListener
	 * @param jsonModel
	 */
	protected <T> void requestGenericSingleObject(final Enum<?> area, final Enum<?> function, final QueryMap qm, final int requestMethod) {
		requestGenericSingleObject(area, function, qm, requestMethod, null, null);
	}

	protected <T> void requestGenericSingleObject(final Enum<?> area, final Enum<?> function, final QueryMap qm, final int requestMethod, final ResultListenerNG<JSonModel> resultListener, final Class<? extends JSonModel> jsonModel) {
		requestDataUsingCache(area, function, requestMethod, qm, new ResultListenerNG<JSONObject>() {
			@Override
			public void onSuccess(JSONObject response) {
				if (resultListener != null) {
					Constructor<?> c;
					try {
						c = jsonModel.getConstructor(new Class[]{JSONObject.class});
						resultListener.onSuccess((JSonModel) c.newInstance(new Object[]{response}));
					} catch (Exception e) {
						Log.e(TAG, "Exception: " + e);
					}
				}
			}

			@Override
			public void onError(ErrorModel error) {
				if (resultListener != null)
					resultListener.onError(error);
			}
		});
	}


	// MSH: Temp method while we wait for correct url Otherwise we should overload the correct way!
	protected void requestFile(final String url, final String localFilename, final ResultListenerNG<File> resultListener) {
		Log.d(TAG, "url: " + url);
		final Request<File> r = new FileRequest(Request.Method.GET, url, null, localFilename, 
			new Response.Listener<File>() {
				@Override
				public void onResponse(File response) {
					hideProgressBar();
					resultListener.onSuccess(response);
				}
			},
			new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					hideProgressBar();
					resultListener.onError(processVolleyError(error));
				}
			}
		);

		// MSH: Change timeout policy. - http://stackoverflow.com/questions/17094718/android-volley-timeout
		if (DefaultRetryPolicy.DEFAULT_TIMEOUT_MS != Config.DEFAULT_TIMEOUT_MS)
			r.setRetryPolicy(new DefaultRetryPolicy(Config.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

		processRequest(r);
	}

	protected void requestFile(final Enum<?> area, final Enum<?> function, final int requestMethod, final QueryMap qm, final String localFilename, final ResultListenerNG<File> resultListener) {
		applyQueryTemplate(qm);
		final String url = generateUrl(area, function, qm.toString());
		Log.d(TAG, "url: " + url);
		final Request<File> r = new FileRequest(requestMethod, url, null, localFilename, 
			new Response.Listener<File>() {
				@Override
				public void onResponse(File response) {
					hideProgressBar();
					resultListener.onSuccess(response);
				}
			},
			new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					hideProgressBar();
					resultListener.onError(processVolleyError(error));
				}
			}
		);

		// MSH: Change timeout policy. - http://stackoverflow.com/questions/17094718/android-volley-timeout
		if (DefaultRetryPolicy.DEFAULT_TIMEOUT_MS != Config.DEFAULT_TIMEOUT_MS)
			r.setRetryPolicy(new DefaultRetryPolicy(Config.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

		processRequest(r);
	}

	
	
	protected <T> void requestGenericArray(final Enum<?> area, final Enum<?> function, final QueryMap qm, final ResultListenerNG<ArrayList<T>> resultListener, final Class<? extends JSonModel> jsonModel) {
		applyQueryTemplate(qm);
		final int cacheTimeout = processCacheTimeout(qm);
		Log.d(TAG, "cacheTimeout: " + cacheTimeout);
		final String url = generateUrl(area, function, qm != null ? qm.toString() : "");
		final CacheManager cacheManager = new CacheManager(md5(url), cacheTimeout);

		// Lookup data in cache.
		if (cacheManager.isCached()) {
			Log.d(TAG, "Cache hit");
			try {
				final JSONArray response = new JSONArray(cacheManager.readCache());
				// Show cached result regardless of expiration date. If expired a new request will be made and the data will get updarted once the server returns the data.
				final ProcessJSONResponseArrayTask<T> processResponseTask = new ProcessJSONResponseArrayTask<T>(resultListener, jsonModel);
				processResponseTask.execute(response);
			} catch (JSONException e) {
				Log.e(TAG, "JSONException: " + e);
			}

			// Has cache expired?
			if (cacheManager.isExpired()) {
				Log.d(TAG, "Cache has expired - request new data");
			} else {
				Log.d(TAG, "Cache is still valid - no need to request new data");
				return;
			}
		} else
			Log.d(TAG, "Cache miss");

		requestData(url, new ResultListenerNG<JSONArray>() {
			@Override
			public void onSuccess(JSONArray response) {
				cacheManager.writeCache(response);
				// MSH: Process result on background thread.
				final ProcessJSONResponseArrayTask<T> processResponseTask = new ProcessJSONResponseArrayTask<T>(resultListener, jsonModel);
				processResponseTask.execute(response);
			}

			@Override
			public void onError(ErrorModel error) {
				if (resultListener != null)
					resultListener.onError(error);
			}
		});
	}

	/**
	 * Cancel all pending requests tagged with this activity.
	 * This should be called when an activity is move to the background ie onPause.
	 */
	public void cancelAll() {
		synchronized (this) {
			RequestManager.getRequestQueue().cancelAll(this.getActivity());
			this.progressBarCount = 0;
			setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Return activity from weak reference.
	 *
	 * @return
	 */
	public Activity getActivity() {
		return this.activity.get();
	}

	/**
	 * Methods for handling auth params used by the Request classes.
	 */
	public static void putAuthParam(final String key, final String value) {
		authParams.put(key, value);
	}

	public static Map<String, String> getAuthParams() {
		return authParams;
	}

	public static void clearAuthParams() {
		authParams.clear();
	}
}