package fr.gabuzomeu.canadeche;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yann on 03/08/13.
 */
public class BoardFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";
        public static final String ARG_SECTION_ID = "section_id";
        public CanadecheService mBoundService = null;
        Button refreshButton;
        Button postButton;
        MessagesDataSource mds;
        ArrayAdapter<Message> adapter;

        private static String TAG="Canadeche BoardFragment";
        ListView messagesListView;
        //static ArrayList<ArrayAdapter> adaptersList = new ArrayList();
        List<Message> messages;
        String boardName;

    public BoardFragment( ) {

        }

        public BoardFragment( CanadecheService service) {
            mBoundService = service;
            Log.d( TAG, "mBoundService " + mBoundService);
        }


        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        public String getBoardName(){
            return boardName;
        }

        public void displayTotoz( String _totoz){

            LayoutInflater inflater = LayoutInflater.from( getActivity());
            AlertDialog.Builder imageDialog = new AlertDialog.Builder( getActivity());


            View layout = inflater.inflate(R.layout.totoz_popup, null);
            ImageView image = (ImageView) layout.findViewById(R.id.fullimage);

            new DownloadImageTask( image).execute("http://totoz.eu/img/" + _totoz);
            imageDialog.setTitle( "[:" + _totoz + "]" );
            imageDialog.setView( layout);
            imageDialog.setPositiveButton( R.string.ok_button, new DialogInterface.OnClickListener(){

                public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
                }

            });


            imageDialog.create();
            imageDialog.show();
        }



        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            boardName = getArguments().getString(ARG_SECTION_ID);
            Log.d(TAG, "IN CREATEVIEW " + getArguments().getString(ARG_SECTION_ID));

            final View rootView = inflater.inflate(R.layout.fragment_main_board, container, false);
            //    TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            //  dummyTextView.setText("PLOP " + Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

            refreshButton =(Button)rootView.findViewById( R.id.refreshButton);
            refreshButton.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), getArguments().getString(ARG_SECTION_ID), Toast.LENGTH_SHORT).show();
                    mBoundService.refresh( getArguments().getString(ARG_SECTION_ID ));
                    refreshContent();
                }
            });

            postButton =(Button)rootView.findViewById( R.id.postButton);
            postButton.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    EditText et = (EditText)rootView.findViewById( R.id.message);
                    String mess = et.getText().toString();
                    postMessage(  getArguments().getString(ARG_SECTION_ID), mess);
                    et.setText("");
                    mBoundService.refresh( getArguments().getString(ARG_SECTION_ID ));
                    refreshContent();
                    Log.d( TAG, "BOARD: " + getArguments().getString(ARG_SECTION_ID) + " MESSAGE: " + mess );
                }
            });



            messagesListView = (ListView)getActivity().findViewById( R.id.messagesListView);
            mds = new MessagesDataSource( getActivity());
            mds.open();

            messages = mds.getAllMessages( getArguments().getString(ARG_SECTION_ID ));
            adapter = new MessageAdapter( getActivity(), R.layout.messagerow, messages, boardName, (EditText)rootView.findViewById( R.id.message));

            Log.d(TAG, "IN CREATEVIEW " + getArguments().getString(ARG_SECTION_ID) + " ADAPTER: " + adapter);
            messagesListView = (ListView)rootView.findViewById( R.id.messagesListView);
            messagesListView.setAdapter( adapter);
            //adaptersList.add( adapter);
            return rootView;
        }

        @Override
        public void onResume() {
            mds.open();
            super.onResume();
        }

        @Override
        public void onPause() {
            mds.close();
            super.onPause();
        }


        public void refreshContent(){
            Log.d(TAG, "adapter " + adapter);
            mds.open();
            messages.clear();
            messages.addAll(mds.getAllMessages(boardName));
            adapter.notifyDataSetChanged();
        }


        /**TODO Use asynctask to post and remove policy things*/
        private boolean postMessage( String board, String message){
            String utf8Message;
            try {
                utf8Message = new String( message.getBytes( "UTF-8") );
            } catch (UnsupportedEncodingException e1) {
                utf8Message = message;
                Log.i( TAG, "Content encoding: " + e1.getMessage());
            }

            message=utf8Message;

            String postUrl = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString( "boardconfig_" + board +"_edittext_boardposturl", null);
            Log.d(TAG, "posturl: " + postUrl);
            Log.d(TAG, "message: " + message);
            HttpPost post = new HttpPost( postUrl);
            String cook = PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_boardcookie", null);
            Log.d(TAG, "COOKIE: " + cook);
            post.addHeader(new BasicHeader("Cookie", PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_boardcookie", null)));
            //post.addHeader(new BasicHeader("Cookie", "remember_account_token=BAhbB1sGaQJhCkkiIiQyYSQxMCRNYzRnd2JQUjVBNnB4MFRkLkxqVXR1BjoGRVQ%3D--8725ab6eef74bd32761dfbcc20c906273ea6fc7b; linuxfr.org_session=BAh7CEkiD3Nlc3Npb25faWQGOgZFVEkiJTFlZTdkZDYyZDBkZmM1MjZlYmFiNDEwMWM1YmQ3OWU4BjsAVEkiHHdhcmRlbi51c2VyLmFjY291bnQua2V5BjsAVFsHWwZpAmEKSSIiJDJhJDEwJE1jNGd3YlBSNUE2cHgwVGQuTGpVdHUGOwBUSSIQX2NzcmZfdG9rZW4GOwBGSSIxMkZua1pZdUxnWEJSWGJjQUJtOE1kK3ppRTZrZ1p1LzIzS1ozK3lWVnl0Yz0GOwBG--422d9fc6f8a5c75e40602642d865baaef9b760f9"));

            post.addHeader(new BasicHeader("Referer", PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_referer", null)));
            post.addHeader(new BasicHeader("Host", PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_host", null)));
            post.addHeader(new BasicHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
            post.addHeader(new BasicHeader("Accept-Language","fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3"));
            post.addHeader(new BasicHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.7"));
            post.addHeader(new BasicHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8"));

            String ua =  PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_boardua", MainActivityDrawer.appName + " " +  MainActivityDrawer.appVersion);
            post.setHeader("User-Agent", ua );

            HttpClient hClient = new DefaultHttpClient();

            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);


                nameValuePairs.add( new BasicNameValuePair( "board[message]", message));
                nameValuePairs.add( new BasicNameValuePair( "board[object_type]", "Free"));
                nameValuePairs.add( new BasicNameValuePair( "board[object_id]", ""));
                nameValuePairs.add( new BasicNameValuePair( "message", message));

                UrlEncodedFormEntity ent = new UrlEncodedFormEntity(nameValuePairs , HTTP.UTF_8 );
                post.setEntity( ent);

                HttpResponse response;
                response=hClient.execute( post);
                //     Log.i( TAG, "RESPONSE-----------> " + response.getStatusLine());
                //    Log.i( TAG, "RESPONSE-----------> " + response.getStatusLine().getReasonPhrase());

                return true;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }





    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {

        bmImage.setImageBitmap( result);

    }
    };

}



