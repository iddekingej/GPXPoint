package org.elaya.gac;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

/**
 * This activity displays a help page.
 */

public class HelpActivity extends Activity {

    /***
     * Setup the GUI
     *
     * The help page is displayed in a WebView.
     * Set the location of the help page
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        WebView lHelp=findViewById(R.id.helpHtml);
        lHelp.loadUrl("file:///android_asset/help.html");
    }

    /**
     * On top there is a "close" button. When pressing this
     * button this method is called and the activity is closed.
     *
     * @param pButton Not used.
     */
    public void closePressed(View pButton){
        finish();
    }
}
