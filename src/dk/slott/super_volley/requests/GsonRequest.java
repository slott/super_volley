package dk.slott.super_volley.requests;

import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import dk.slott.super_volley.config.Config;
import dk.slott.super_volley.managers.DataManagerHelper;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
 
/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class GsonRequest<T> extends JsonRequest<T> {
	private static final String TAG = GsonRequest.class.getSimpleName();
	private final Gson gson;
	private final Class<T> clazz;
	private final Listener<T> listener;

	/**
	 * Make a request and return a parsed object from JSON.
	 * Note that this request object supports caching!
	 *
	 * @param url URL of the request to make
	 * @param clazz Relevant class object, for Gson's reflection
	 * @param headers Map of request headers
	 */
	public GsonRequest(final int method, final String url, final JSONObject jsonRequest, final Class<T> clazz, final int ttl, final Listener<T> listener, final ErrorListener errorListener) {
		super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);

		// MSH: Define the timeformat used in the returned json response.
		this.gson = new GsonBuilder().setDateFormat(Config.DATE_FORMAT).create();
		this.clazz = clazz;
		this.listener = listener;
	}

	/**
	 * MSH: Collect headers that are sent to the server (Request Headers)
	 * Note that we append headers to the header array from the super class.
	 */
	@Override
	public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
		final Map<String, String> params = new HashMap<String, String>();
		final Iterator<Entry<String, String>> it = DataManagerHelper.getAuthParams().entrySet().iterator();

		// MSH: POST auth params. 
		if(Config.AUTH_METHOD == Config.AuthMethod.POST) {
			while (it.hasNext()) {
				final Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
				Log.d(TAG, "auth param: " + entry.getKey() + ": " + entry.getValue());
				params.put(entry.getKey(), entry.getValue());
			}
		}
		/**
		 * MSH: HTTP BASIC AUTH.
		 * http://stackoverflow.com/questions/16817980/how-does-one-use-basic-authentication-with-this-library
		 * http://stackoverflow.com/questions/1968416/how-to-do-http-authentication-in-android
		 * Can't really use key,value combo here so might have to rething this...
		 * TODO: Should just have a method where we define a http basic auth and thats it - no key values!
		 */
		else {
			while (it.hasNext()) {
				final Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
				final String creds = String.format("%s:%s",entry.getKey(),entry.getValue());
				final String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
				params.put("Authorization", auth);
			}
		}
		return params;
	};

	@Override
	protected void deliverResponse(final T response) {
		this.listener.onResponse(response);
	}

	/**
	 * MSH:
	 * Normally we would get a valid cacheEntry when parsing cache headers but because of "Varnish" we
	 * get no-cache and thus a null cache entry. Because of this we do not store the received etag and
	 * thus never send it back to the server when requesting new data.
	 */
	@Override
	protected Response<T> parseNetworkResponse(final NetworkResponse response) {
		
		Log.d(TAG, "parseNetworkResponse");

		// MSH: Ensure ETag is processed correctly by parseCacheHeaders.
		if(response.headers.get("ETag") != null) {
			Log.d(TAG, "Adding missing Cache-Control header to allow etag to be handled correctly");
			// http://www.mnot.net/cache_docs/
			response.headers.put("Cache-Control", "max-age=86400; must-revalidate");
		}

		// MSH: Stream parse response to avoid converting it to a String first.
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(response.data);
		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		final JsonReader reader = new JsonReader(inputStreamReader);
		final T o = gson.fromJson(reader, this.clazz);
		return Response.success(o, HttpHeaderParser.parseCacheHeaders(response));
	}

	/**
	 * MSH: Let the server know what we are sending.
	 */
	@Override
	public String getBodyContentType() {
		return Config.REQUEST_BODY_CONTENT_TYPE.toString() + "; charset=" + getParamsEncoding();
	}

	/**
	 * MSH: Convert supplied JSON body to a well formed POST body if request body content type is FORM
	 */
	@Override
	public byte[] getBody() {
		if(Config.REQUEST_BODY_CONTENT_TYPE == Config.BodyContentType.FORM) {
			byte[] body = super.getBody();
			if(body != null) {
				final String jsonBody = new String(body);
				final List<BasicNameValuePair> paramsAsList = new ArrayList<BasicNameValuePair>();
				try {
					final JSONObject jObject = new JSONObject(jsonBody);
					final Iterator<?> keys = jObject.keys();
					while(keys.hasNext()) {
						final String key = (String)keys.next();
						final Object value = jObject.get(key);
						if(value instanceof Integer)
							paramsAsList.add(new BasicNameValuePair(key, (((Integer)value)+"")));
						else
							paramsAsList.add(new BasicNameValuePair(key, value.toString()));
					}
					return URLEncodedUtils.format(paramsAsList, getParamsEncoding()).getBytes(getParamsEncoding());
				}
				catch (Exception e) {
					Log.e(TAG, "Exception: " + e);
				}
			}
		}
		// MSH: Default data is already json
		else 
			return super.getBody();

		return null;
	}
}
