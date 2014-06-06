package dk.slott.super_volley.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;

import HttpMultipartMode.SimpleMultipartEntity;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import dk.slott.super_volley.managers.QueryMap;

// http://stackoverflow.com/questions/16797468/how-to-send-a-multipart-form-data-post-in-android-with-volley
public class MultipartRequest extends Request<String> {
	private SimpleMultipartEntity entity = new SimpleMultipartEntity();

	private static final String FILE_PART_NAME = "upfile";
	private static final String TAG = MultipartRequest.class.getSimpleName();
	private final Response.Listener<String> mListener;
	private final byte[] mFilePart;
	private QueryMap queryMap;
//	private final String mStringPart;

	public MultipartRequest(final String url, final QueryMap queryMap, Response.ErrorListener errorListener, Response.Listener<String> listener, byte[] file) {
		super(Method.POST, url, errorListener);

		this.mListener = listener;
		this.mFilePart = file;
		this.queryMap = queryMap;

		buildMultipartEntity();
	}

	private void buildMultipartEntity() {
		Log.d(TAG, "buildMultipartEntity");
		entity.addFile(FILE_PART_NAME, mFilePart,"application/zip");

		Log.d(TAG, "Adding query map entries to multipart request.");
		for(Entry<String, Object> entry : this.queryMap.entrySet())
		if(entry.getValue() != null)
			entity.addPart(entry.getKey(), entry.getValue().toString());
	}

	@Override
	public String getBodyContentType() {
		Log.d(TAG, "getBodyContentType");
		return entity.getContentType().getValue();
	}

	@Override
	public byte[] getBody() throws AuthFailureError {
		Log.d(TAG, "getBody");
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			entity.writeTo(bos);
		}
		catch (IOException e) {
			VolleyLog.e("IOException writing to ByteArrayOutputStream");
		}
		return bos.toByteArray();
	}

	@Override
	protected Response<String> parseNetworkResponse(NetworkResponse response) {
		Log.d(TAG, "parseNetworkResponse");
		try {
			final String data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			Log.d(TAG, "data: " + data);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException: " + e);
		}
		return Response.success("Uploaded", getCacheEntry());
	}

	@Override
	protected void deliverResponse(String response) {
		Log.d(TAG, "deliverResponse: " + response);
		this.mListener.onResponse(response);
	}
}