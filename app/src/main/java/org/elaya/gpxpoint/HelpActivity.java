package org.elaya.gpxpoint;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import org.elaya.gpxpoint.R;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        WebView lHelp=(WebView)findViewById(R.id.helpHtml);
        lHelp.loadUrl("file:///android_asset/help.html");
    }

    public void closePressed(View pButton){
        finish();
    }
}
