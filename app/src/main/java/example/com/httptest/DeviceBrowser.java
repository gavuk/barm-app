package example.com.httptest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DeviceBrowser extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_browser);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String url = intent.getStringExtra(MainActivity.BROWSER_URL);

        // Capture the layout's TextView and set the string as its text
        WebView webView = findViewById(R.id.webView);

        // Don't open in a separate browser
        webView.setWebViewClient(new WebViewClient());

        //This statement is used to enable the execution of JavaScript.
        webView.getSettings().setJavaScriptEnabled(true);

        //This statement hides the Vertical scroll bar and does not remove it.
        webView.setVerticalScrollBarEnabled(false);

        //This statement hides the Horizontal scroll bar and does not remove it.
        webView.setHorizontalScrollBarEnabled(false);

        // Load the page
        webView.loadUrl("http://" + url + ":3000");
    }
}
