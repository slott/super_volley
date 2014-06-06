package dk.slott.super_volley.requests;

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import dk.slott.super_volley.config.Config;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class EnhancedStringRequest extends Request<String> {
	private static final String TAG = EnhancedStringRequest.class.getSimpleName();
	private final Listener<String> mListener;
	private JSONObject jsonRequest;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param method the request {@link Method} to use
	 * @param url URL to fetch the string at
	 * @param listener Listener to receive the String response
	 * @param errorListener Error listener, or null to ignore errors
	 */
	public EnhancedStringRequest(int method, String url, final JSONObject jsonRequest, Listener<String> listener, ErrorListener errorListener) {
		super(method, url, errorListener);
		this.mListener = listener;
		this.jsonRequest = jsonRequest;
	}

	/**
	 * Creates a new GET request.
	 *
	 * @param url URL to fetch the string at
	 * @param listener Listener to receive the String response
	 * @param errorListener Error listener, or null to ignore errors
	 */
	public EnhancedStringRequest(String url, final JSONObject jsonRequest, Listener<String> listener, ErrorListener errorListener) {
		this(Method.GET, url, jsonRequest, listener, errorListener);
	}

	@Override
	protected void deliverResponse(String response) {
		mListener.onResponse(response);
	}

	@Override
	protected Response<String> parseNetworkResponse(NetworkResponse response) {
		String parsed;
		try {
			parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
		} catch (UnsupportedEncodingException e) {
			parsed = new String(response.data);
		}
		return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
	}
/*
	@Override
	protected String getParamsEncoding() {
		return "base64";
	}
*/	
	/**
	 * MSH: Convert supplied JSON body to a well formed POST body if request body content type is FORM
	 */
	@Override
	public byte[] getBody() {
		final String jsonBody = new String(this.jsonRequest.toString());
		if(Config.REQUEST_BODY_CONTENT_TYPE == Config.BodyContentType.FORM) {
			final List<BasicNameValuePair> paramsAsList = new ArrayList<BasicNameValuePair>();
			try {
				final JSONObject jObject = new JSONObject(jsonBody);
				final Iterator<?> keys = jObject.keys();
				while(keys.hasNext()) {
					final String key = (String)keys.next();
					final Object value = jObject.get(key);
					if(value instanceof Integer)
						paramsAsList.add(new BasicNameValuePair(key, ((Integer)value)+""));
					else
						paramsAsList.add(new BasicNameValuePair(key, value.toString()));
				}

				final String urlParams = URLEncodedUtils.format(paramsAsList, getParamsEncoding());
				Log.d(TAG, "urlParams: " + urlParams);
				return urlParams.getBytes(getParamsEncoding());
			}
			catch (Exception e) {
				Log.e(TAG, "Exception: " + e);
			}
		}

		try {
			return jsonBody.getBytes(getParamsEncoding());
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException: " + e);
		}
		return null;
	}
}
