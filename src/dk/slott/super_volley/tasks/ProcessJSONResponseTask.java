package dk.slott.super_volley.tasks;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import dk.slott.super_volley.managers.ResultListenerNG;
import dk.slott.super_volley.models.JSonModel;
import android.os.AsyncTask;
import android.util.Log;

public class ProcessJSONResponseTask<T> extends AsyncTask<JSONObject, Void, ArrayList<T>> {
	private static final String TAG = ProcessJSONResponseTask.class.getSimpleName();

	private ResultListenerNG<ArrayList<T>> resultListener;
	private Class<? extends JSonModel> theClassInArrayList;
	
	public ProcessJSONResponseTask(final ResultListenerNG<ArrayList<T>> resultListener, final Class<? extends JSonModel> theClassInArrayList) {
		this.resultListener = resultListener;
		this.theClassInArrayList = theClassInArrayList;
	}
	
	@Override
	protected ArrayList<T> doInBackground(JSONObject... params) {
		Log.d(TAG, "onPostExecute");
		final ArrayList<T> items = new ArrayList<T>();
		try {
			final JSonModel instance =  (JSonModel) Class.forName(theClassInArrayList.getCanonicalName()).newInstance();
			
			// MSH: The array denoted by the provided name ie. movies or tvseries.
			final JSONArray jsonArray = params[0].getJSONArray(	instance.getResultType());
		
			final Constructor<?> c = this.theClassInArrayList.getConstructor(new Class[] { JSONObject.class });	
			
			// MSH: Each item is instantiated as the approiate object and added to the result array.
			for(int n=0; n < jsonArray.length(); n++) {
				@SuppressWarnings("unchecked")
				final T a = (T) c.newInstance(new Object[] { jsonArray.getJSONObject(n) });
				items.add(a);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSONException: " + e);
		} catch (Exception e) {
			Log.e(TAG, "Exception: " + e);
		}

		return items;

	}
	
	@Override
	protected void onPostExecute(ArrayList<T> result) {
		super.onPostExecute(result);
		Log.d(TAG, "onPostExecute");
		resultListener.onSuccess(result);
		
	}

}
