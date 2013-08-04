package fr.gabuzomeu.canadeche;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by yann on 31/07/13.
 */
public class CanadecheService extends Service {

    public static final String TAG = "CanadecheService";
    public static final String REFRESH_BOARD = "REFRESH_BOARD";
    private static Timer timer = new Timer();

    private ArrayList boardsId = new ArrayList();

    boolean debug;

    @Override
    public void onCreate() {
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_linuxfr, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_gabuzomeu, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_euromussels, false);
        PreferenceManager.setDefaultValues( this, R.xml.boardconfig_see, false);
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false);
        debug = PreferenceManager.getDefaultSharedPreferences( this).getBoolean( "pref_debug", false);
        Log.d( TAG, "Debug is "  + debug);
        super.onCreate();
        startService();
    }

    private void startService(){
        String secondsAsString = PreferenceManager.getDefaultSharedPreferences( this).getString( "pref_service_update_seconds", "300");
        int seconds = Integer.parseInt( secondsAsString);
        timer.scheduleAtFixedRate(new mainTask(), 0, seconds * 1000);
    }


    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC. (http://developer.android.com/reference/android/app/Service.html#LocalServiceSample)
     */

    public class LocalBinder extends Binder {
        CanadecheService getService() {
            return CanadecheService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if( debug){
            Log.d(TAG, ">>>Client connected");
        }
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public void plop(){
        if( debug){
            Log.d(TAG, ">>>PLOP");
        }
    }

    public void refresh( String boardId){
        if( debug){
            Log.d(TAG, ">>>REFRESH FORCED " + boardId);
        }
            Intent intent = new Intent("refresh-board");
            // You can also include some extra data.
            intent.putExtra("message", boardId + " has been refreshed :)");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            new updateAsyncTask().execute();
    }



    private class mainTask extends TimerTask{
        public void run(){
                Log.d(TAG, ">>>REFRESH ");
                reloadBoardsConfig();
                if( getMainLooper() == null)
                    Looper.prepare();
                new updateAsyncTask().execute();

                }
     };


    class updateAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground( Void... arg0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            boolean sticky = prefs.getBoolean( "pref_stickyservice", true);
            if( sticky)
                fetchNewPosts( true);
            return null;
        }
    }


    public void reloadBoardsConfig(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boardsId.clear();
        Map<String,?> keys = prefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            if( entry.getKey().contains( "checkbox_boardenabled") && Boolean.parseBoolean( entry.getValue().toString()) == true){
                StringTokenizer st = new StringTokenizer( entry.getKey(), "_");
                st.nextToken();
                String boardId = st.nextToken();
                boardsId.add( boardId);

                String boardUrl = prefs.getString( "boardconfig_" + boardId +"_edittext_boardslipurl", null );
                if( debug)
                    Log.d(TAG,  "Need to fetch boardId " + boardUrl);
            }
        }
    }



    public int fetchNewPosts( boolean all){

        int newmessCounter=0;

        for( int i=0; i < boardsId.size(); i++){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String boardBackenUrl = prefs.getString( "boardconfig_" + boardsId.get( i) +"_edittext_boardslipurl", null );

            try{
                URL url = new URL( boardBackenUrl );
                File tmpFile = File.createTempFile( (String)boardsId.get( i), "canadeche", getApplicationContext().getCacheDir());
                if( debug)
                    Log.d( TAG, tmpFile.getAbsolutePath());
                BufferedInputStream in = new BufferedInputStream(url.openStream());
                FileOutputStream fos = new FileOutputStream( tmpFile.getPath());
                BufferedOutputStream bout = new BufferedOutputStream( fos,1024);
                byte[] data = new byte[1024];
                int x=0;
                while((x=in.read(data,0,1024))>=0){
                    bout.write(data,0,x);
                }
                bout.close();
                in.close();

                /***/

                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();

                BoardParser bParser = new BoardParser();
                xr.setContentHandler( bParser);
                FileReader fr = new FileReader( tmpFile);
                xr.parse(new InputSource( fr));
                tmpFile.delete();
                ArrayList<Message> messageList = bParser.getMessages();
                Iterator<Message> it = messageList.iterator();

                MessagesDataSource mds = new MessagesDataSource( this);
                mds.open();
                    while( it.hasNext()){
                        Message mess = it.next();
                        mess.setBoard( (String)boardsId.get( i));
                        long postId = mds.createMessage( mess);
                        if( postId != -1)
                            newmessCounter++;
                }
                mds.close();

            }catch( Exception e){
                Log.d( TAG, "Oups: " + e.toString() + " -- "  + e.getMessage());
            }
        }
        if( newmessCounter > 0){
            Intent intent = new Intent("refresh-board");
            intent.putExtra("message", "SERVICE REFRESH");
            LocalBroadcastManager.getInstance( getBaseContext()).sendBroadcast( intent);
        }
        return 1;

    }


}
