package dk.slott.super_volley.stacks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HurlStack;


public class ApacheStack extends HurlStack {
	private static final String TAG = ApacheStack.class.getSimpleName();
	private final DefaultHttpClient client;
	private final static String HEADER_CONTENT_TYPE = "Content-Type";

	public ApacheStack() {
		this(new DefaultHttpClient());
	}

	public ApacheStack(DefaultHttpClient client) {
		this.client = new DefaultHttpClient();
		this.client.addResponseInterceptor(new HttpResponseInterceptor() {
			@Override
			public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				Log.d(TAG, "process");
			}
		});
	}

	@Override
	protected HttpURLConnection createConnection(URL url) throws IOException {
		Log.d(TAG, "createConnection: " + url);
		return super.createConnection(url);
	}

	@Override
	public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
		//Cache.Entry cacheEntry = request.getCacheEntry();
		//if(cacheEntry != null) {
		//	Log.d(TAG, "cacheEntry TTL: " + cacheEntry.ttl);
		//}
		
		// TODO: We can add the session id here and remove it from the JsonObjectExtendedRequest class!
		
		// Put caching logic here too and avoid having it in the Request class! This would make for a very clean cut.
		// See if we can use a cache header rather than out current cacheTimeout header which we remove...
		// We cant remove it if we put the logic here unless there is a dedicated cache header/method/param available...
		// Kig paa TTL 
	
		final HttpUriRequest httpRequest = createHttpRequest(request, additionalHeaders);
		addHeaders(httpRequest, additionalHeaders);
		addHeaders(httpRequest, request.getHeaders());
		onPrepareRequest(httpRequest);
		final HttpParams httpParams = httpRequest.getParams();
		final int timeoutMs = request.getTimeoutMs();
		// TODO: Reevaluate this connection timeout based on more wide-scale
		// data collection and possibly different for wifi vs. 3G.
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

		// MSH: Get Response to extract the session id from the cookie header. 
		final HttpResponse httpResponse = client.execute(httpRequest); 
/*
		try {
			// This is the correct cookie header!
			final Header h = httpResponse.getLastHeader("Set-Cookie");
			String sessionId = h.getValue().split(";")[0].split("=")[1];
			Log.d(TAG, "Header sessionId2: " + sessionId);
			//DataManagerHelper.sessionId = sessionId;
			DataManagerNG.sessionID = sessionId;
		}
		catch (Exception e) {
			Log.w(TAG, "Unable to extract session id");
		}
*/
		// the first volley of cookies are not the ones we are looking for...
		//final Header h2 = httpResponse.getFirstHeader("Set-Cookie");
		//String sessionId2 = h2.getValue().split(";")[0].split("=")[1];
		//Log.d(TAG, "Header sessionId2: " + sessionId2);

		return httpResponse;
	}

	/**
	 * Creates the appropriate subclass of HttpUriRequest for passed in request.
	 */
	@SuppressWarnings("deprecation")
	protected static HttpUriRequest createHttpRequest(Request<?> request, Map<String, String> additionalHeaders) throws AuthFailureError {
		switch (request.getMethod()) {
		case Method.DEPRECATED_GET_OR_POST: {
			// This is the deprecated way that needs to be handled for backwards compatibility.
			// If the request's post body is null, then the assumption is that the request is
			// GET.  Otherwise, it is assumed that the request is a POST.
			byte[] postBody = request.getPostBody();
			if (postBody != null) {
				HttpPost postRequest = new HttpPost(request.getUrl());
				postRequest.addHeader(HEADER_CONTENT_TYPE, request.getPostBodyContentType());
				HttpEntity entity;
				entity = new ByteArrayEntity(postBody);
				postRequest.setEntity(entity);
				return postRequest;
			} else {
				return new HttpGet(request.getUrl());
			}
		}
		case Method.GET:
			return new HttpGet(request.getUrl());
		case Method.DELETE:
			return new HttpDelete(request.getUrl());
		case Method.POST: {
			HttpPost postRequest = new HttpPost(request.getUrl());
			postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
			setEntityIfNonEmptyBody(postRequest, request);
			return postRequest;
		}
		case Method.PUT: {
			HttpPut putRequest = new HttpPut(request.getUrl());
			putRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
			setEntityIfNonEmptyBody(putRequest, request);
			return putRequest;
		}
		default:
			throw new IllegalStateException("Unknown request method.");
		}
	}

	private static void setEntityIfNonEmptyBody(HttpEntityEnclosingRequestBase httpRequest,
			Request<?> request) throws AuthFailureError {
		byte[] body = request.getBody();
		if (body != null) {
			HttpEntity entity = new ByteArrayEntity(body);
			httpRequest.setEntity(entity);
		}
	}
	private static void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
		for (String key : headers.keySet()) {
			httpRequest.setHeader(key, headers.get(key));
		}
	}

	/**
	 * Called before the request is executed using the underlying HttpClient.
	 *
	 * <p>Overwrite in subclasses to augment the request.</p>
	 */
	protected void onPrepareRequest(HttpUriRequest request) throws IOException {
		// Nothing.
	}
}