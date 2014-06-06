package dk.slott.super_volley.managers;

public interface ResultListenerArray<T> {
	public void onSuccess(final T response);
	public void onError(final String error);

}
