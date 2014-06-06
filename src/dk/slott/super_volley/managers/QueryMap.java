package dk.slott.super_volley.managers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class QueryMap extends LinkedHashMap<String, Object> implements Map<String, Object>{
	private static final long serialVersionUID = -7607844916041020445L;
	private static final String TAG = QueryMap.class.getSimpleName();
	private String queryTemplate;

	// MSH: magic sauce :)
	private String generateQuerySet(final String key, final Object value, final String queryTemplate) {
		try {
			return String.format(queryTemplate, key, URLEncoder.encode(value.toString(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException: " + e);
			return String.format(queryTemplate, key, value.toString());
		}
	}

	public void setQueryTemplate(final String queryTemplate) {
		this.queryTemplate = queryTemplate;
	}

	@Override
	public String toString() {
		final StringBuffer urlParams = new StringBuffer(120);
		if (this!=null) {
			final Set<Entry<String, Object>> s=this.entrySet();
			final Iterator<Entry<String, Object>> it=s.iterator();
			while(it.hasNext()) {
				final Map.Entry<String, Object> m =(Map.Entry<String, Object>)it.next();
				// MSH: Ignore null values - but leave a log warning.
				if(m.getValue() == null)
					Log.w(TAG, "Input value is null for key: " + m.getKey());
				else
					urlParams.append(generateQuerySet(m.getKey(), m.getValue(), this.queryTemplate));
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
}
