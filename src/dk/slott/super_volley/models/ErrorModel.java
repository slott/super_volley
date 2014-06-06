package dk.slott.super_volley.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.android.volley.VolleyError;
import com.google.gson.JsonParseException;

public class ErrorModel {
	private static final String TAG = ErrorModel.class.getSimpleName();
	private int errorNumber;
	private String errorMsg;
	private int statusCode;

	public ErrorModel() {}
	public ErrorModel(JSONObject jo) {}
	public ErrorModel(VolleyError volleyError) throws JSONException {
		if(volleyError.networkResponse != null) {
			setStatusCode(volleyError.networkResponse.statusCode);
			final String networkResponse = new String(volleyError.networkResponse.data);

			// MSH: Default show network response.
			this.errorMsg = networkResponse;

			try {
				// MSH: This will throw an exception if response code is 500 and provided data is garbage ie. non JSON.
				final JSONObject error = new JSONObject(networkResponse);
				if(error.has("errorcode"))
					try {
						this.setErrorNumber(error.getInt("errorcode"));
					} catch (JSONException e) {
						Log.e(TAG, "JSONException: " + e);
					}
				if(error.has("error"))
					try {
						this.setErrorMsg(error.getString("error"));
					} catch (JSONException e) {
						Log.e(TAG, "JSONException: " + e);
					}
			}
			catch (JSONException e) {
				Log.w(TAG, "JSONException: " + e);
				// MSH: Maybe do this for all...
				if(statusCode == 500) {
					this.errorMsg = networkResponse;
					this.errorNumber = -1; // Error 0 causes "no internet connection" toast message.
				}
			}
			
		}
		else if(volleyError.getCause() instanceof JsonParseException)
			this.errorMsg = volleyError.getMessage();
		else
			this.errorMsg = volleyError.getMessage();
	}

	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return (this.errorMsg != null) ? this.errorMsg : "";
	}
	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	/**
	 * @return the errorNumber
	 */
	public int getErrorNumber() {
		return this.errorNumber;
	}
	/**
	 * @param errorNumber the errorNumber to set
	 */
	public void setErrorNumber(int errorNumber) {
		this.errorNumber = errorNumber;
	}
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return this.statusCode;
	}
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getId() {
		return "";
	}
}
