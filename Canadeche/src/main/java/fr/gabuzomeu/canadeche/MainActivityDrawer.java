package fr.gabuzomeu.canadeche;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivityDrawer extends Activity {

    public static String TAG = "Canadeche MainActivityDrawer";
    ViewPager mViewPager;
    Boolean debug = false;
    private boolean mIsBound;
    private static CanadecheService mBoundService;


    public static String appName = "Canadeche";
    public static String appVersion="0.1";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener prefsListener;
    private ArrayList enabledBoardsNameList = new ArrayList();

    ArrayAdapter<String> drawerBoardsListAdapter;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    BoardFragment currentFragment;

    private boolean calledForSharing = false;


    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(android.os.Message msg) {
            ArrayList returnMessage = (ArrayList)msg.obj;
            String boardName = (String)returnMessage.get( 0);
            int position = Integer.parseInt((String) returnMessage.get(1));
            String url = (String)returnMessage.get( 2);
            selectItem( position, boardName, url);
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);


        final Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_linuxfr, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_gabuzomeu, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_euromussels, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_see, false);
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);



        debug = prefs.getBoolean("pref_debug", false);
        Log.d( TAG, "Debug is "  + debug);

        prefsListener =  new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged( SharedPreferences prefs, String key) {
                        if( debug)
                            Log.d("TAG", "Preferences " + key + " has been changed");

                        if( key.compareTo( "boards_enabled_reorder_timestamp") == 0 ){
                            Log.d("TAG", "Boards order changed " );
                            enabledBoardsNameList.clear();
                            enabledBoardsNameList.addAll( readBoardsListFromPreferences());
                            drawerBoardsListAdapter.notifyDataSetChanged();
                        }

                        if( key.contains( "checkbox_boardenabled")){
                            Log.d("TAG", "Board state changed: " + key );
                            refreshEnabledBoardsNames();
                            enabledBoardsNameList.clear();
                            enabledBoardsNameList.addAll( readBoardsListFromPreferences());
                            drawerBoardsListAdapter.notifyDataSetChanged();
                            if( currentFragment != null)
                                mBoundService.refresh( currentFragment.getBoardName() );
                        }



                    }
                };

        prefs.registerOnSharedPreferenceChangeListener(prefsListener);

        doBindService();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("refresh-board"));

        if( PreferenceManager.getDefaultSharedPreferences( this).getBoolean( "pref_fullscreen", false) ){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }



        setContentView(R.layout.activity_main_drawer);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);


        enabledBoardsNameList = readBoardsListFromPreferences();
        //Should be called at first start
        if( enabledBoardsNameList.size() == 0)
            enabledBoardsNameList = refreshEnabledBoardsNames();


        drawerBoardsListAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, enabledBoardsNameList);
        mDrawerList.setAdapter( drawerBoardsListAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());




        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
                getActionBar().setTitle( currentFragment.getBoardName());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled( true);
        getActionBar().setHomeButtonEnabled( true);

        /**Todo Remove that*/
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //




        if( debug){
            Log.d(TAG, "Received Intent: Action: " + action + " type: " + type );
        }
        if (Intent.ACTION_SEND.equals( intent.getAction()) && intent.getType() != null) {

            calledForSharing = true;

            if ("text/plain".equals( intent.getType())) {
                final String sentText = intent.getStringExtra( Intent.EXTRA_TEXT);
                if( debug)
                    Log.d( TAG, "Received intent text/plain : " + sentText);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select board for sharing");

                final ListView boardListView = new ListView(this);
                ArrayAdapter<String> boardListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, enabledBoardsNameList);
                boardListView.setAdapter( boardListAdapter);

                builder.setView( boardListView);
                final Dialog dialog = builder.create();

                boardListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                        String boardName = (String)boardListView.getItemAtPosition( position);
                        selectItem( position, boardName, sentText);
                        Toast.makeText( getApplicationContext(), "You selected : " + boardName,Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
            else{
                Log.d( TAG, "Upload file " + intent.getType());

//Todo: Handle orientation changes
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select board for sharing");

                final ListView boardListView = new ListView(this);
                ArrayAdapter<String> boardListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, enabledBoardsNameList);
                boardListView.setAdapter( boardListAdapter);

                builder.setView( boardListView);
                final Dialog dialog = builder.create();

                boardListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                        String boardName = (String)boardListView.getItemAtPosition( position);
                        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (imageUri != null) {
                            String filePath = convertMediaUriToPath( imageUri);
                            if( debug)
                                Log.d("TAG", "in handleUploadFile: " + imageUri + " --> " + filePath);
                            HttpMultipartPost task = new HttpMultipartPost();
                            task.execute( filePath, boardName, String.valueOf( position));
                         }
                         dialog.dismiss();

                    }
                });

                dialog.show();

            }

        }

    }

    @Override
    protected void onNewIntent(Intent intent)
    {

        super.onNewIntent(intent);
        if( intent.getScheme() != null){
            if( intent.getScheme().compareTo( "totoz") == 0){
                String totoz = intent.getDataString();
                totoz = totoz.substring( 10, totoz.length()-1);
                if( debug)
                    Toast.makeText( getApplicationContext(), "Totoz received: " + totoz, Toast.LENGTH_SHORT).show();
                currentFragment.displayTotoz( totoz );
            }else if ( intent.getScheme().compareTo( "norloge") == 0){
                String norloge = intent.getDataString();
                norloge = norloge.substring( 10, norloge.length());
                if( debug)
                    Toast.makeText( getApplicationContext(), "Norloge received: " + norloge, Toast.LENGTH_SHORT).show();
                currentFragment.filterOn( null);
                currentFragment.displaySearchBar( norloge);
            }

        }



    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView tv= (TextView)view.findViewById( R.id.drawer_element_textview);
            selectItem( position, tv.getText().toString(), null);
        }
    }

    //prepMessage cand be used to prefill post filed
    private void selectItem(int position, String boardName, String prepMessage) {
        if( debug){
            Log.d(TAG, "selectItem() : " + position + " " + boardName);
        }

        currentFragment = new BoardFragment( mBoundService, prepMessage);
        Bundle args = new Bundle();
        args.putInt( BoardFragment.ARG_SECTION_NUMBER, position);
        args.putString(BoardFragment.ARG_SECTION_ID, boardName);
        currentFragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, currentFragment).commit();

        mDrawerList.setItemChecked( position, true);
        getActionBar().setTitle( (String)enabledBoardsNameList.get( position));
        mDrawerLayout.closeDrawer(mDrawerList);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    /**Browse all sharedPreferences to find which boards are enabled, should be used one time, at first launch*/
    public ArrayList refreshEnabledBoardsNames(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        ArrayList boardNamesArray = new ArrayList();

        Map<String,?> keys = prefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            if( entry.getKey().contains( "checkbox_boardenabled") && Boolean.parseBoolean( entry.getValue().toString()) == true){
                StringTokenizer st = new StringTokenizer( entry.getKey(), "_");
                st.nextToken();
                String boardId = st.nextToken();
                boardNamesArray.add( boardId);
                if( debug)
                    Log.d("BOARDS",  boardId + " ENABLED");
            }
        }

        //Store the enabled boards in preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt( "boards_enabled_size", boardNamesArray.size());
        int size = boardNamesArray.size();

        if( size > 0){
            for( int i = 0; i < size; i++){
                editor.putString( "boards_enabled_" + i, (String)boardNamesArray.get( i) );
                if( debug)
                    Log.d( TAG, "New board settings wrote to prefs: " + i + " " + (String)boardNamesArray.get( i));
            }
            editor.commit();
        }
    return boardNamesArray;
    }


    private ArrayList  readBoardsListFromPreferences(){

        ArrayList list = new ArrayList();
        int size = prefs.getInt( "boards_enabled_size", 0);

        for( int i =0; i < size; i++){
            Log.d( TAG, "Read Board["+i+"] >> " + prefs.getString( "boards_enabled_" + i, "empty"));
            list.add( prefs.getString( "boards_enabled_" + i, "empty"));
        }

    return list;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch(item.getItemId()) {
            case R.id.action_refresh:
                mBoundService.refresh( currentFragment.getBoardName() );
                return true;
            case R.id.action_search:
                currentFragment.displaySearchBar( null);
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }


    /*Connection to service*/
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((CanadecheService.LocalBinder)service).getService();
            Log.d( TAG, "SERVICE connected: " + mBoundService);
            if( debug)
                Toast.makeText( MainActivityDrawer.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
            mBoundService.plop();
            if( !calledForSharing)
                selectItem( 0, (String)enabledBoardsNameList.get( 0), null);
            calledForSharing = false;
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText( MainActivityDrawer.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };



    void doBindService() {
        bindService(new Intent( this, CanadecheService.class), mConnection, this.BIND_AUTO_CREATE);
       // Log.d( TAG, "")
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
            String message = intent.getStringExtra("message");
            if( debug){
                Log.d("receiver", "Got message: " + message);
                Toast.makeText( MainActivityDrawer.this, String.valueOf( System.currentTimeMillis()) , Toast.LENGTH_SHORT).show();
            }
            if( currentFragment != null){
                if( debug)
                    Log.d( TAG, "Current fragment is refreshed");
                currentFragment.refreshContent();
            }
        }
    };



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private class HttpMultipartPost extends AsyncTask< String, Integer, String>
    {
        ProgressDialog pd;
        long totalSize;

        String boardName;
        String boardPosition;

        @Override
        protected void onPreExecute()
        {
            pd = new ProgressDialog( MainActivityDrawer.this);
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setMessage("Uploading Picture...");
            pd.setCancelable( true);
            pd.show();
        }

        @Override
        protected String doInBackground( String... arg0)
        {
            boardName = arg0[1];
            boardPosition = arg0[2];

            HttpClient httpClient = new DefaultHttpClient();
            HttpContext httpContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost("http://plop.cc/attach.php");

            try
            {
                CustomMultiPartEntity multipartContent;

                multipartContent = new CustomMultiPartEntity(new CustomMultiPartEntity.ProgressListener()
                {
                    @Override
                    public void transferred(long num)
                    {
                        publishProgress((int) ((num / (float) totalSize) * 100));
                    }
                });

                // We use FileBody to transfer an image
                multipartContent.addPart("attach_file", new FileBody(new File(arg0[0])));
                totalSize = multipartContent.getContentLength();

                // Send it
                httpPost.setEntity( multipartContent);
                HttpResponse response = httpClient.execute(httpPost, httpContext);
                String serverResponse = EntityUtils.toString(response.getEntity());
                if( debug)
                    Log.d(TAG, "Server Response -- > " + serverResponse);
                Pattern pattern = Pattern.compile( ",\\ \"(.*)\"");
                Matcher matcher = pattern.matcher( serverResponse);
                String url = "";
                if( matcher.find()){
                    url = matcher.group(1);
                }
                if( debug)
                    Log.d( TAG, "URLLLLLLLLLLLLLLLLL->" + url);

                return url;
            }

            catch (Exception e)
            {
                System.out.println(e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            pd.setProgress((int) (progress[0]));
        }

        @Override
        protected void onPostExecute(String ui){
            Toast.makeText( MainActivityDrawer.this, "PostExecute: " + ui, Toast.LENGTH_SHORT).show();
            ArrayList messageContent = new ArrayList();
            messageContent.add( 0, boardName);
            messageContent.add( 1, String.valueOf( boardPosition));
            messageContent.add( 2, ui);
            android.os.Message msg = new Message();
            msg.obj = messageContent;
            mHandler.dispatchMessage( msg);
            pd.dismiss();
        }
    }



    protected String convertMediaUriToPath(Uri uri) {

        String path = uri.toString();

        String [] proj={ MediaStore.Images.Media.DATA};
        if( debug)
            Log.d(TAG, "Trying to convert: " + uri);

        if( uri.toString().startsWith( "file://")){
            path = uri.toString().substring(7);
            return path;
        }

        Cursor cursor = getContentResolver().query(uri, proj,  null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        path = cursor.getString(column_index);
        cursor.close();
        return path;
    }





}
