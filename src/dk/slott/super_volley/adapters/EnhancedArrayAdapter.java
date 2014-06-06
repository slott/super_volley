package dk.slott.super_volley.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import dk.slott.super_volley.models.JSonModel;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

/**
 * Add backwards support or addAll and an all new replaceAll that only notifies if data has changed to avoid flicker.
 * @author mortenslotthansen
 *
 * @param <T>
 */
public class EnhancedArrayAdapter<T extends JSonModel> extends ArrayAdapter<T> {
	private static final String TAG = EnhancedArrayAdapter.class.getSimpleName();
	protected LayoutInflater inflater;

	public EnhancedArrayAdapter(Context context) {
		this(context, new ArrayList<T>());
	}
	public EnhancedArrayAdapter(Context context, List<T> data) {
		super(context, 0, data);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/**
	 * Allow pre-HONEYCOMB to use addAll.
	 * http://stackoverflow.com/questions/9677172/listviews-how-to-use-arrayadapter-addall-function-before-api-11
	 */	
	@SuppressLint("NewApi")
	@Override
	public void addAll(Collection<? extends T> collection) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			super.addAll(collection);
		else {
			for(T c : collection)
				super.add(c);
		}
	}
	
	public boolean hasDataChanged(final Collection<T> collection) {
		if(getCount() > 0) {

			// MSH: Assert if the items are the same
			if(getCount() == collection.size()) {
				// MSH: Loop through the list and register change as soon as one element differs.
				final Iterator<T> i = collection.iterator();
				for(int n=0 ; n<getCount() ; n++) {
					final String aId = i.next().getId();
					final String bId = getItem(n).getId();

					// MSH: Abort loop as soon as a difference is found.
					if(aId == null && bId == null) {
						// do nothing, its the facebook cover
					} else if (aId == null) { // existing item is a facebook cover but is replaced with a movie item
						return true;
					} else {
						if(!aId.equals(bId)) {
							return true;
						}
					}
				}
				return false;
			}
			// MSH: Different size means there was a change.
			else {
				Log.d(TAG, "Different size means there was a change.getCount() = " + getCount() + " collection.size() = " + collection.size());
				return true;
			}
		}
		// No data to display.
		else if(collection.size() == 0 && getCount() == 0){
			Log.d(TAG, "collection.size() == 0 - lets update it.");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * MSH: Takes care of clearing the array and notifying data manager - if any changes.
	 * @param collection
	 */
	public void replaceAll(final Collection<T> collection) {
		Log.d(TAG, "replaceAll.....");
		clear();
		addAll(collection);
		notifyDataSetChanged();
	}
}
