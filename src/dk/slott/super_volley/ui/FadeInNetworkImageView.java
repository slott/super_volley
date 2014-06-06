package dk.slott.super_volley.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

public class FadeInNetworkImageView extends NetworkImageView {
	private static final int FADE_IN_TIME_MS = 200;

	public FadeInNetworkImageView(Context context) {
		super(context);
	}

	public FadeInNetworkImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		TransitionDrawable td = new TransitionDrawable(new Drawable[]{
				new ColorDrawable(android.R.color.transparent),
				new BitmapDrawable(getContext().getResources(), bm)
		});

		setImageDrawable(td);
		td.startTransition(FADE_IN_TIME_MS);
	}
}
