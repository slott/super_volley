package dk.slott.super_volley.config;

import com.android.volley.DefaultRetryPolicy;

/**
 * Defailt config values - can all be overridden by app.
 * @author mortenslotthansen
 *
 */
public class Config {
	public static enum BodyContentType {
		JSON("application/json"), FORM("application/x-www-form-urlencoded");
		private String string;
		BodyContentType(final String string) {
			this.string = string;
		}
		@Override 
		public String toString() { 
			return string; 
		} 
	}
	public static enum AuthMethod {POST, HTTP_BASIC_AUTH}

	public static String SERVER_ADDRESS = "";
	// Alternative pattern example: {server}/{area}/?action={function}{parameters} 
	public static String URL_PATTERN = "%s/%s/%s/format/json%s"; // {server}/{area}/{function}/format/json{parameters}
	// Alternative pattern example: &{key}={value}
	public static String QUERY_PATTERN = "/%s/%s"; // /{key}/{value} 
	public static String UA_NUMBER = "change this";
	public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static int DEFAULT_TIMEOUT_MS = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
	public static BodyContentType REQUEST_BODY_CONTENT_TYPE = BodyContentType.FORM;
	public static AuthMethod AUTH_METHOD = AuthMethod.POST;
}
