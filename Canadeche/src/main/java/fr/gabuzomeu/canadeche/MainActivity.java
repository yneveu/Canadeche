package fr.gabuzomeu.canadeche;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;


import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

public class MainActivity extends FragmentActivity {

    SectionsPagerAdapter mSectionsPagerAdapter;

    public static String TAG = "Canadeche MainActivity";
    ViewPager mViewPager;
    Boolean debug = false;
    private boolean mIsBound;
    private CanadecheService mBoundService;

    public static String appName = "Canadeche";
    public static String appVersion="0.1";



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        debug = PreferenceManager.getDefaultSharedPreferences( this).getBoolean( "pref_debug", false);
        Log.d( TAG, "Debug is "  + debug);

        doBindService();
        LocalBroadcastManager.getInstance(this).registerReceiver( mMessageReceiver, new IntentFilter( "refresh-board"));

        super.onCreate(savedInstanceState);
        if( PreferenceManager.getDefaultSharedPreferences( this).getBoolean( "pref_fullscreen", false) ){
            //requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {


        ArrayList boardNamesArray = new ArrayList();
        ArrayList fragments = new ArrayList();


        @Override public Parcelable saveState() { return null; }

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            Map<String,?> keys = prefs.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){
                if( entry.getKey().contains( "checkbox_boardenabled") && Boolean.parseBoolean( entry.getValue().toString()) == true){
                    StringTokenizer st = new StringTokenizer( entry.getKey(), "_");
                    st.nextToken();
                    String boardId = st.nextToken();
                    boardNamesArray.add( boardId);
                    //this.notifyDataSetChanged();

                    Log.d("BOARDS",  boardId + " ENABLED");
                }
            }

        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = new BoardSectionFragment( mBoundService );
            fragments.add( fragment);
            Bundle args = new Bundle();
            args.putInt( BoardSectionFragment.ARG_SECTION_NUMBER, position + 1);
            args.putString(BoardSectionFragment.ARG_SECTION_ID, (String) boardNamesArray.get(position));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {

            return boardNamesArray.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (String)boardNamesArray.get( position);

        }

        public ArrayList getFragments(){
            return fragments;

        }

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }



    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public static class BoardSectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";
        public static final String ARG_SECTION_ID = "section_id";
        public CanadecheService mBoundService = null;
        Button refreshButton;
        Button postButton;
        MessagesDataSource mds;
        ArrayAdapter<Message> adapter;

        ListView messagesListView;
        //static ArrayList<ArrayAdapter> adaptersList = new ArrayList();


        public BoardSectionFragment( ) {

        }

        public BoardSectionFragment( CanadecheService service) {
            mBoundService = service;
        }


        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);



        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

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
                    Log.d( TAG, "BOARD: " + getArguments().getString(ARG_SECTION_ID) + " MESSAGE: " + mess );
                }
            });



            messagesListView = (ListView)getActivity().findViewById( R.id.messagesListView);
            mds = new MessagesDataSource( getActivity());
            mds.open();

            List<Message> messages = mds.getAllMessages( getArguments().getString(ARG_SECTION_ID ));
            adapter = new MessageAdapter( getActivity(), R.layout.messagerow, messages, "plop",null);

           // adapter = new ArrayAdapter<Message>( getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, messages);


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

            String postUrl = PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_boardposturl", null);
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

            String ua =  PreferenceManager.getDefaultSharedPreferences( getActivity()).getString( "boardconfig_" + board +"_edittext_boardua", appName + " " + appVersion);
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

    }

/*Connection to service*/


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((CanadecheService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText( MainActivity.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
            mBoundService.plop();
            // Create the adapter that will return a fragment for each of the three
            // primary sections of the app.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);



        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText( MainActivity.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };



    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent( this, CanadecheService.class), mConnection, this.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        doUnbindService();
        super.onDestroy();
    }



    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            if( debug){
                Log.d("receiver", "Got message: " + message);
                Toast.makeText( MainActivity.this, String.valueOf( System.currentTimeMillis()) , Toast.LENGTH_SHORT).show();
            }


            if( mSectionsPagerAdapter != null){

                ArrayList fragments = mSectionsPagerAdapter.getFragments();
                int nbFragments = fragments.size();

                for( int i=0; i < nbFragments; i++ ){
                    BoardSectionFragment bsf = (BoardSectionFragment)fragments.get(i);
                    bsf.refreshContent();
                }
            }

        }
    };






}
