package dk.slott.super_volley.requests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonArrayRequest;
import dk.slott.super_volley.managers.DataManagerHelper;

/**
 * Applies the required headers to allow request to be authenticated.
 * @author mortenslotthansen
 *
 */
public class JsonArrayAuthRequest extends JsonArrayRequest {
	public JsonArrayAuthRequest(String url, Listener<JSONArray> listener, ErrorListener errorListener) {
		super(url, listener, errorListener);
	}

	@Override
	public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
		final Map<String, String> params = new HashMap<String, String>();

		// MSH: Forward auth params. 
		final Iterator<Entry<String, String>> it = DataManagerHelper.getAuthParams().entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
			params.put(entry.getKey(), entry.getValue());
		}

		return params;
	};
}
