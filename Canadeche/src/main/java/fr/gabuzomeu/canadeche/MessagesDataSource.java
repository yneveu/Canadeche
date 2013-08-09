package fr.gabuzomeu.canadeche;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yann on 01/08/13.
 */
public class MessagesDataSource {

    private static String TAG = "CanadecheMessagesDataSource";
    boolean debug = false;


    private SQLiteDatabase database;
    private SqliteHelper dbHelper;

    private String[] allColumns = {
            SqliteHelper.COLUMN_BOARD_ID,
            SqliteHelper.COLUMN_ID,
            SqliteHelper.COLUMN_TIME,
            SqliteHelper.COLUMN_INFO,
            SqliteHelper.COLUMN_MESSAGE,
            SqliteHelper.COLUMN_LOGIN,
            SqliteHelper.COLUMN_POST_ID
             };

    private Context context;
    SharedPreferences prefs;

    public static final String COLUMN_BOARD_ID = "board_id";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_INFO = "info";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_POST_ID = "post_id";

    public MessagesDataSource(Context _context) {
        context = _context;
        dbHelper = new SqliteHelper( context);
        prefs = PreferenceManager.getDefaultSharedPreferences( context);
        debug = prefs.getBoolean("pref_debug", false);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }


    public long createMessage( Message inputMessage ){
        ContentValues values = new ContentValues();

        String selectQuery = "SELECT " + SqliteHelper.COLUMN_BOARD_ID + "," + SqliteHelper.COLUMN_POST_ID + " FROM " + SqliteHelper.MESSAGES_TABLE + " WHERE " +
                SqliteHelper.COLUMN_BOARD_ID + "=='" + inputMessage.getBoard() + "' AND " + SqliteHelper.COLUMN_POST_ID + " == " + inputMessage.getId();

        Cursor cursor = database.rawQuery( selectQuery , null);

        if( cursor.getCount() == 0){

            if( debug){
                Log.d(TAG, "Ho, a new post --> " + inputMessage.toString());
            }

            values.put( SqliteHelper.COLUMN_LOGIN, inputMessage.getLogin());
            values.put( SqliteHelper.COLUMN_INFO, inputMessage.getInfo());
            values.put( SqliteHelper.COLUMN_MESSAGE, inputMessage.getMessage());
            values.put( SqliteHelper.COLUMN_POST_ID, inputMessage.getId());
            values.put( SqliteHelper.COLUMN_TIME, inputMessage.getTime());
            values.put( SqliteHelper.COLUMN_BOARD_ID, inputMessage.getBoard());
            long insertId = database.insert( SqliteHelper.MESSAGES_TABLE, null, values);
            cursor.close();

            String comparedLogin = prefs.getString( "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin", "improbablestring87984qsfqezfeqz" ) + "<";
            String comparedLoginEncoded = prefs.getString( "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin", "improbablestring87984qsfqezfeqz" ) + "&amp;lt;";
            if( inputMessage.getMessage().contains( comparedLogin ) || inputMessage.getMessage().contains( comparedLoginEncoded )  ){
                if( debug)
                    Log.d( TAG, "Bigorno  " + "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin") ;
                    notifyOnNewPost( inputMessage, "New personal message on board" + inputMessage.getBoard() );
            }


            if( prefs.getBoolean( "boardconfig_"+ inputMessage.getBoard() + "_checkbox_quietboard", false ) ){

                if( debug)
                    Log.d( TAG, "Quietboard  " + "boardconfig_"+ inputMessage.getBoard() + "_checkbox_boardlogin") ;
                notifyOnNewPost( inputMessage, "New message on board" + inputMessage.getBoard() );
            }
            return insertId;
        }
        cursor.close();
        return -1;
    }





        private void notifyOnNewPost( Message inputMessage, String reason){
            final NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent intent = new Intent( context, MainActivityDrawer.class);
            PendingIntent pIntent = PendingIntent.getActivity( context, 0, intent, 0);

            //Format norloge
            String sClock= String.valueOf( inputMessage.getTime());
            final String hour=sClock.substring( 8, 10);
            final String minutes=sClock.substring(10,12);
            final String seconds=sClock.substring( 12,14);
            String cNorloge =  hour + ":" + minutes + ":" + seconds;

            Notification noti =  new NotificationCompat.Builder( context)
                    .setContentTitle(reason)
                    .setContentText(inputMessage.getBoard() + " (" + cNorloge + ") : " + inputMessage.getMessage())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLights(Color.YELLOW, 500, 500)
                    .setContentIntent(pIntent).build();

            noti.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(0, noti);
        }

/**TODO: limit par tribune*/
    public List<Message> getAllMessages( String inputBoard){
       List<Message> messages = new ArrayList<Message>();
       Cursor cursor = database.query( SqliteHelper.MESSAGES_TABLE, allColumns, SqliteHelper.COLUMN_BOARD_ID + " == '" + inputBoard +"'", null, null, null, SqliteHelper.COLUMN_TIME, null);

       String postsAsString = PreferenceManager.getDefaultSharedPreferences( context).getString( "pref_max_post_displayed", "100");
       int maxPosts = Integer.parseInt( postsAsString);

       if( cursor.getCount() < maxPosts )
            cursor.moveToFirst();
        else
           cursor.moveToPosition( cursor.getCount() - maxPosts );

        while (!cursor.isAfterLast()) {
            Message mess = new Message();
            mess.setBoard( cursor.getString( 0));
            mess.setId( cursor.getInt( 6));
            mess.setInfo( cursor.getString( 3));
            mess.setLogin( cursor.getString( 5));
            mess.setMessage(cursor.getString( 4));
            mess.setTime( cursor.getLong( 2));
            messages.add( mess);

            cursor.moveToNext();
        }

        cursor.close();
        return messages;
    }

}
