package dk.slott.super_volley.managers;

import dk.slott.super_volley.models.ErrorModel;

public interface ResultListenerNG<T> {
	public void onSuccess(final T response);
	public void onError(final ErrorModel error);
}
