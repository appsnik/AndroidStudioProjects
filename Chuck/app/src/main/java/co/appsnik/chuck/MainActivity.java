package co.appsnik.chuck;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private MyBroadcastReceiver receiver = new MyBroadcastReceiver();

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, receiver.intentfilter);
        ConnectivityChangeObservable.instance().addObserver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ConnectivityChangeObservable.instance().deleteObserver(this);
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override // Observer interface
    public void update(Observable observable, Object data) {
        findViewById(R.id.get_chuck).setEnabled((Boolean) data);
    }

    public void getChuckFact(View view) {
        getRandomJoke();
    }

    public void getRandomJoke() {
        new DownloadTask() {
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if (result != null) {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.getString("type").equals("success")) {
                            JSONObject jsonValue = json.getJSONObject("value");
                            String joke = jsonValue.getString("joke");
                            TextView textbox = (TextView)findViewById(R.id.joke_text_box);
                            textbox.setText(joke);
                        }
                    } catch (JSONException e) {
                        Log.i(TAG, "Failed to parse json object.");
                        e.printStackTrace();
                    }
                }
            }
        }.execute("http://api.icndb.com/jokes/random/");
    }
/*
    public void getJokeCount() {
        new DownloadTask().execute("http://api.icndb.com/categories");
    }
*/
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            InputStream stream = null;

            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setDoInput(true);
                conn.connect();
                stream = conn.getInputStream();
                return convertStreamToString(stream);
            } catch (IOException e) {
                Log.i(TAG, "Failed to download url: " + urls[0]);
                e.printStackTrace();
                return null;
            } finally {
                if (stream != null) {
                    try {
                        Log.i(TAG, "Closing stream");
                        stream.close();
                    } catch (IOException e) {
                    }
                }
                if (conn != null) {
                    Log.i(TAG, "Disconnecting");
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, null != result ? result : "(null)");
        }

        private String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
