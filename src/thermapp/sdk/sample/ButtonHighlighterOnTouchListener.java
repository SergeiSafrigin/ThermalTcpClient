/*
 * Higliting buttons on click class
 */

package thermapp.sdk.sample;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ButtonHighlighterOnTouchListener implements OnTouchListener {

	  private static final int TRANSPARENT_GREY = Color.argb(0, 185, 185, 185);
	  private static final int FILTERED_GREY = Color.argb(155, 185, 185, 185);

	  ImageView imageView;
	  TextView textView;

	  public ButtonHighlighterOnTouchListener(final ImageView imageView) {
	    super();
	    this.imageView = imageView;
	  }

	  public ButtonHighlighterOnTouchListener(final TextView textView) {
	    super();
	    this.textView = textView;
	  }

	  public boolean onTouch(final View view, final MotionEvent motionEvent) {
	    if (imageView != null) {
	      if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
	        imageView.setColorFilter(FILTERED_GREY);
	      } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
	        imageView.setColorFilter(TRANSPARENT_GREY);
	      }
	    } 
	    return false;
	  }

	}