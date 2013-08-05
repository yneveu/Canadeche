package fr.gabuzomeu.canadeche;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

public class MainActivityDrawer extends Activity {

    public static String TAG = "Canadeche MainActivity";
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
    private ArrayList enabledBoardsNameList;

    ArrayAdapter<String> drawerBoardsListAdapter;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    BoardFragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

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
                        mBoundService.refresh( currentFragment.getBoardName() );
                        enabledBoardsNameList.clear();
                        enabledBoardsNameList.addAll( getEnabledBoardsNames());
                        drawerBoardsListAdapter.notifyDataSetChanged();


                    }
                };

        prefs.registerOnSharedPreferenceChangeListener( prefsListener);

        doBindService();
        LocalBroadcastManager.getInstance(this).registerReceiver( mMessageReceiver, new IntentFilter( "refresh-board"));

        if( PreferenceManager.getDefaultSharedPreferences( this).getBoolean( "pref_fullscreen", false) ){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main_drawer);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        enabledBoardsNameList = getEnabledBoardsNames();
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
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        /**Todo Remove that*/
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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
            }
        }
    }


    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView tv= (TextView)view.findViewById( R.id.drawer_element_textview);
            selectItem(position, tv.getText().toString());
        }
    }

    private void selectItem(int position, String boardName) {
        currentFragment = new BoardFragment( mBoundService);
        Bundle args = new Bundle();
        args.putInt( BoardFragment.ARG_SECTION_NUMBER, position);
        args.putString(BoardFragment.ARG_SECTION_ID, boardName);
        currentFragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, currentFragment).commit();

        mDrawerList.setItemChecked(position, true);
        getActionBar().setTitle( (String)enabledBoardsNameList.get( position));
        mDrawerLayout.closeDrawer(mDrawerList);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    /**Browse all sharedPreferences to find which boards are enabled*/
    public ArrayList getEnabledBoardsNames(){
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
    return boardNamesArray;
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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


    /*Connection to service*/
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((CanadecheService.LocalBinder)service).getService();
            Log.d( TAG, "SERVICE connected: " + mBoundService);
            if( debug)
                Toast.makeText( MainActivityDrawer.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
            mBoundService.plop();
            selectItem( 0, (String)enabledBoardsNameList.get( 0));
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
}
