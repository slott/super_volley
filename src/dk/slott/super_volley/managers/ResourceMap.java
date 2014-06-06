package dk.slott.super_volley.managers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class ResourceMap extends LinkedHashMap<String, Object> implements Map<String, Object>{
	private static final long serialVersionUID = 603613924346400931L;
	private static final String TAG = ResourceMap.class.getSimpleName();
	private static final String queryTemplate = "/%s/%s";

	private String generateQuerySet(final String key, final Object value) {
		// MSH: If resource mapping is a collection there will be no value.
		// http://en.wikipedia.org/wiki/Representational_state_transfer
		if(value == null)
			return "/" + key;
		try {
			return String.format(queryTemplate, key, URLEncoder.encode(value.toString(), "UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException: " + e);
			return String.format(queryTemplate, key, value.toString());
		}
	}

	@Override
	public String toString() {
		final StringBuffer urlParams = new StringBuffer(120);
		if (this!=null) {
			final Set<Entry<String, Object>> s=this.entrySet();
			final Iterator<Entry<String, Object>> it=s.iterator();
			while(it.hasNext()) {
				final Map.Entry<String, Object> m =(Map.Entry<String, Object>)it.next();
				urlParams.append(generateQuerySet(m.getKey(), m.getValue()));
			}
		}
		return urlParams.toString();
	}

	/**
	 * MSH: For the internal workings we want to keep key values as type String.
	 * @param key
	 * @param value
	 */
	public void put(final Enum<?> key, final Object value) {
		super.put(key.toString(), value);
	}
	public void put(final Enum<?> key) {
		super.put(key.toString(), null);
	}
	
}
