/*
 * Terms of use class
 */

package thermapp.sdk.sample;

import thermapp.sdk.sample.R;
import android.os.Bundle;
import android.webkit.WebView;
import android.app.Activity;

public class TermsOfUse extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_terms_of_use);
		
		WebView wb = (WebView)findViewById(R.id.webView_terms);
		String url =  "file:///android_asset/terms.html";
		wb.loadUrl(url);
	}
	
}
