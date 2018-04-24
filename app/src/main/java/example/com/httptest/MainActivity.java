package example.com.httptest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    // Constants for browser intent
    public static final String BROWSER_URL = "example.com.httptest.URL";
    public static final String BROWSER_PORT = "example.com.httptest.PORT";

    // Make some variables global
    TextView textIP;
    Button btnScan;
    String urlString;
    String localIP = null;
    LinearLayout deviceLL = null;
    Boolean endScan = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assign the linear layout
        deviceLL = (LinearLayout) findViewById(R.id.deviceListLinear);

        // Assign the Scan button
        btnScan = (Button) findViewById(R.id.buttonScan);

        // Get the current IP of this device
        try {
            localIP = new GetLocalIp().execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the IP text
        textIP = (TextView) findViewById(R.id.textIP);
        textIP.setText(localIP);

        // Call the subnet scanner
        new subnetDeviceScanner().scan(localIP);

        // Scan button listener
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove buttons from the previous scan
                deviceLL.removeAllViews();

                // Un-cancel the scan
                endScan = false;

                // Run the scan
                new subnetDeviceScanner().scan(localIP);
            }
        });
    }

    // Class to get the local IP address
    public class GetLocalIp extends AsyncTask<String, Integer, String>
    {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... params)
        {
            // Declare ip variable
            String ip = null;

            // Get the local IP with a socket connection
            try(final DatagramSocket socket = new DatagramSocket()){
                try {
                    socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ip = socket.getLocalAddress().getHostAddress();
            } catch (SocketException e) {
                e.printStackTrace();
            }

            return ip;
        }
    }

    // Class to go through the subnet and check for devices
    public class subnetDeviceScanner
    {
        protected String scan(String ip)
        {
            // Set the button text
            btnScan.setText(getString(R.string.button_scanning));

            // Split up the IP address
            String[] ipParts = ip.split("\\.");

            // Build the first 3 octets of the subnet address
            String ipFirstPart = null;
            ipFirstPart = ipParts[0] + "." + ipParts[1] + "." + ipParts[2];

            // Set the current list variable
            String currentList = null;

            // Go through the subnet
            for (int i = 0; i < 256; i++) {

                // Stop scanning if a device is chosen
                if (endScan)
                    break;

                // Convert i to string
                String thisOctet = Integer.toString(i);

                // Check if the page is available
                if (!endScan)
                    new checkPage().execute("http://" + ipFirstPart + "." + thisOctet + ":5001", ipFirstPart + "." + thisOctet, thisOctet);
            }

            return null;
        }
    }


    private class checkPage extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            // Make sure the scan hasn't been cancelled
            if (endScan) {
                String[] ret = {"FAIL", params[1]};
                return ret;
            }

            // Change the button text if we're done
            if (params[2] == "255")
                btnScan.setText("Scan");

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String pageJsonStr = null;
            InputStream inputStream = null;

            try {
                // Construct the URL
                String goToUrl = params[0];
                URL url = new URL(goToUrl);

                // Create the request and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(500);
                    urlConnection.connect();

                    // Read the input stream into a String
                    inputStream = urlConnection.getInputStream();
                } catch (UnknownHostException e) {
                    String[] ret = {"FAIL", params[0]};
                    return ret;
                } catch (MalformedURLException e) {
                    String[] ret = {"FAIL", params[0]};
                    return ret;
                }
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                pageJsonStr = buffer.toString();
                String[] ret = {params[1], pageJsonStr};
                return ret;
            } catch (IOException e) {
                Log.e("INFO", "Error ", e);
                String[] ret = {"FAIL", params[1]};
                return ret;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("INFO", "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(final String[] s) {
            super.onPostExecute(s);
            Log.i("json", s[0]);

            // Add a button to the linear layout
            if (s[0] != "FAIL") {
                // Get items from JSON object
                // Define the vars
                JSONObject jObject = null;
                String jName = null;
                int iPort = 0;

                // Parse the JSON
                try {
                    jObject = new JSONObject(s[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    jName = jObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    iPort = jObject.getInt("port");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Convert port number to a string
                final String jPort = Integer.toString(iPort);

                // Create the button
                Button btn = new Button(MainActivity.this);
                btn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                btn.setGravity(Gravity.CENTER | Gravity.RIGHT);
                btn.setText(jName);

                // Set the button listener
                btn.setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        //Stop scanning
                        endScan = true;

                        // Get the URL
                        Button b = (Button) v;
                        //String url = b.getText().toString();
                        String url = s[0];

                        // Open the browser
                        Intent intent = new Intent(MainActivity.this, DeviceBrowser.class);
                        intent.putExtra(BROWSER_URL, url);
                        intent.putExtra(BROWSER_PORT, jPort);
                        startActivity(intent);

                    }
                });
                deviceLL.addView(btn);
            }

        }
    }
}