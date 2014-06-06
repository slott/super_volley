package dk.slott.super_volley.requests;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import dk.slott.super_volley.MainApplication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
 
/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class FileRequest extends Request<File> {
	private static final String TAG = FileRequest.class.getSimpleName();
	private final Listener<File> listener;
	private final String filename;

	/**
	 * Make a request and return a parsed object from JSON.
	 * Note that this request object supports caching!
	 *
	 * @param filename Filename on device.
	 */
	public FileRequest(final int method, final String url, final JSONObject jsonRequest, final String filename, final Listener<File> listener, final ErrorListener errorListener) {
		super(method, url, errorListener);
		this.listener = listener;
		this.filename = filename;
	}

	@Override
	protected void deliverResponse(final File response) {
		this.listener.onResponse(response);
	}

	/**
	 */
	@Override
	protected Response<File> parseNetworkResponse(final NetworkResponse response) {
		OutputStream out = null;
		InputStream in = null;
		try {
			Log.d(TAG, "parseNetworkResponse");
			final File configFile = new File(MainApplication.FILE_REQUEST_PATH, this.filename);
			byte[] buf=new byte[1024];
			int bytes_read = -1;
			out = new FileOutputStream(configFile, false);
			in = new ByteArrayInputStream(response.data);
			while ((bytes_read = in.read(buf)) != -1) {
				out.write(buf, 0, bytes_read);
			}
			out.flush();
			
			return Response.success(configFile, HttpHeaderParser.parseCacheHeaders(response));
		} 
		catch (Exception e) {
			return Response.error(new ParseError(e));
		}
		finally {
			if(out != null)
				try {
					out.close();
				} catch (IOException e) {
					Log.e(TAG, "IOException: " + e);
				}
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					Log.e(TAG, "IOException: " + e);
				}
		}
	}

	/**
	 * MSH: Let the server know what we are sending.
	 */
	@Override
	public String getBodyContentType() {
		return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
	}

	/**
	 * MSH: Convert supplied JSON body to a well formed POST body.
	 * @throws AuthFailureError 
	 */
	@Override
	public byte[] getBody() throws AuthFailureError {
		byte[] body = super.getBody();
		if(body != null) {
			final String jsonBody = new String(body);
			final List<BasicNameValuePair> paramsAsList = new ArrayList<BasicNameValuePair>();
			try {
				final JSONObject jObject = new JSONObject(jsonBody);
				final Iterator<?> keys = jObject.keys();
				while(keys.hasNext()) {
					final String key = (String)keys.next();
					paramsAsList.add(new BasicNameValuePair(key.toLowerCase(Locale.ENGLISH), ((String)jObject.get(key))));
				}
				return URLEncodedUtils.format(paramsAsList, getParamsEncoding()).getBytes(getParamsEncoding());
			}
			catch (Exception e) {
				Log.e(TAG, "Exception: " + e);
			}
		}
		return null;
	}
/*
	public void arrayOfBytesToFile( String[] args ) {
	    	FileInputStream fileInputStream=null;
	 
	        File file = new File("C:\\testing.txt");
	 
	        byte[] bFile = new byte[(int) file.length()];
	 
	        try {
	            //convert file into array of bytes
		    fileInputStream = new FileInputStream(file);
		    fileInputStream.read(bFile);
		    fileInputStream.close();
	 
		    //convert array of bytes into file
		    FileOutputStream fileOuputStream = new FileOutputStream("C:\\testing2.txt"); 
		    fileOuputStream.write(bFile);
		    fileOuputStream.close();
	 
		    System.out.println("Done");
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }
	    */
}
