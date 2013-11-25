package fr.gabuzomeu.canadeche;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;

/**
 * Created by yann on 15/11/13.
 */
public class TotozManager extends Activity {

    private static String totozServer="http://nsfw.totoz.eu";
    private static String TAG="canadechetotozmanager";

    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.totozmanager);
        setTitle( "Totoz Manager");

        String returnText = readTotozSearchResponse( true, null);
        WebView wView = (WebView) findViewById( R.id.totozManagerWebView);
        WebSettings webSettings = wView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wView.addJavascriptInterface( this, "Android");



        wView.loadData( returnText, "text/html", "UTF-8");


        final Button searchButton = (Button) findViewById(R.id.searchTotozButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String returnText = readTotozSearchResponse( false, ((EditText)findViewById( R.id.searchTotozEditText)).getText().toString());
                WebView wView = (WebView) findViewById( R.id.totozManagerWebView);
                wView.loadData( returnText, "text/html", "UTF-8");
                //Log.d( TAG, returnText);
            }
        });


        super.onCreate(savedInstanceState);
    }

    public String readTotozSearchResponse( boolean latestTotoz, String searchTerm) {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        String requestUrl ="";

        if( !latestTotoz )
            requestUrl = totozServer + "/search.json?terms=" + Uri.encode( searchTerm);
        else
            requestUrl = totozServer + "/latest.json";


        Log.d( TAG, "REQUEST! : " + requestUrl);
        HttpGet httpGet = new HttpGet( requestUrl );
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e( TAG, "Failed to download file");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String webPage = "<html><head></head><script type=\"text/javascript\">\n" +
                "    function chooseTotoz( totoz) {\n" +
                "        Android.chooseTotoz( totoz);\n" +
                "    }\n" +
                "</script><body><ul>";
        if( latestTotoz)
            webPage += "<h1>Latest totozes</h1>";
        try {
            JSONObject jsonObject = new JSONObject( builder.toString());

            JSONArray jsonArray =  jsonObject.getJSONArray( "totozes");
            Log.i( TAG, "Number of entries " + jsonArray.length());

            if( jsonArray.length() == 0){
                return "<html><body>No totoz found <div><img  src=\"http://totoz.eu/img/bad%20news\" alt=\"\"></div></body></html>";
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject totozJsonObject = jsonArray.getJSONObject(i);
                webPage = webPage + "<li style=\"list-style-type: none;background-color:#fff8fd;\" ><img onClick='chooseTotoz(\"[:" + totozJsonObject.getString( "name") + "]\")' style=\"height: auto; max-width:100px;\" src=\"" + totozJsonObject.getString( "url") + "\"/> [:" + totozJsonObject.getString( "name") +  "]</li>";
                //Log.i( TAG, totozJsonObject.getString("name"));
            }
            webPage += "</ul></body></html>";


        } catch (Exception e) {
            e.printStackTrace();
        }
        return webPage;
    }


    /** Show a toast from the web page */
    @JavascriptInterface
    public void chooseTotoz(String totoz) {
        Log.d( TAG, "in showToast");
        Toast.makeText( this, totoz, Toast.LENGTH_SHORT).show();
        Intent returnIntent = new Intent();
        returnIntent.putExtra( "totoz", totoz);
        Log.d( TAG, "before finish");
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnIntent);
        }
        else {
            getParent().setResult(Activity.RESULT_OK, returnIntent);
        }
        finish();
    }


}