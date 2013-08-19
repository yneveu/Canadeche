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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    public long createMessage( Missive inputMessage ){
        ContentValues values = new ContentValues();

        String selectQuery = "SELECT " + SqliteHelper.COLUMN_BOARD_ID + "," + SqliteHelper.COLUMN_POST_ID + " FROM " + SqliteHelper.MESSAGES_TABLE + " WHERE " +
                SqliteHelper.COLUMN_BOARD_ID + "=='" + inputMessage.getBoard() + "' AND " + SqliteHelper.COLUMN_POST_ID + " == " + inputMessage.getId();

        Cursor cursor = database.rawQuery( selectQuery , null);
        //Log.d(TAG, "Parse new post ,My login here: " + prefs.getString( "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin", null) + " Message: " + inputMessage.toString());
        if( cursor.getCount() == 0){

            if( debug){
                Log.d(TAG, "Ho, a new post ,My login here: " + prefs.getString( "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin", null) + " Message: " + inputMessage.toString());
            }

            values.put( SqliteHelper.COLUMN_LOGIN, inputMessage.getLogin());
            values.put( SqliteHelper.COLUMN_INFO, inputMessage.getInfo());
            values.put( SqliteHelper.COLUMN_MESSAGE, inputMessage.getMessage());
            values.put( SqliteHelper.COLUMN_POST_ID, inputMessage.getId());
            values.put( SqliteHelper.COLUMN_TIME, inputMessage.getTime());
            values.put( SqliteHelper.COLUMN_BOARD_ID, inputMessage.getBoard());
            long insertId = database.insert( SqliteHelper.MESSAGES_TABLE, null, values);
            cursor.close();

            String boardLogin = prefs.getString( "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin", null);
            if( boardLogin != null){

            String comparedLogin = new String( boardLogin + "<").toLowerCase();
            String comparedLoginEncoded = new String( boardLogin +  "&amp;lt;").toLowerCase();

            if( inputMessage.getMessage().toLowerCase().contains( comparedLogin ) || inputMessage.getMessage().toLowerCase().contains( comparedLoginEncoded )  ){
                if( debug)
                    Log.d( TAG, "Bigorno  " + "boardconfig_"+ inputMessage.getBoard() + "_edittext_boardlogin") ;
                notifyOnNewPost( inputMessage, "New personal message on board" + inputMessage.getBoard() );
            }
            }

            if( prefs.getBoolean( "boardconfig_"+ inputMessage.getBoard() + "_checkbox_quietboard", false ) ){

                if( debug)
                    Log.d( TAG, "Quietboard  " + "boardconfig_"+ inputMessage.getBoard() + "_checkbox_boardlogin") ;
                notifyOnNewPost( inputMessage, "New message on board " + inputMessage.getBoard() );
            }




              /*Find reefrences to other posts and fill answers table if needed*/
            //Norloges
            //Pour l'instant pas de multitribune ni de gestion des jours précédents ni de post à la même seconde...
            //Pattern norlogesPattern = Pattern.compile("((?:1[0-2]|0[1-9])/(?:3[0-1]|[1-2][0-9]|0[1-9])#)?((?:2[0-3]|[0-1][0-9])):([0-5][0-9])(:[0-5][0-9])?([¹²³]|[:\\^][1-9]|[:\\^][1-9][0-9])?(@[A-Za-z0-9_]+)?");
            Pattern norlogesPattern = Pattern.compile("(((?:2[0-3]|[0-1][0-9])):([0-5][0-9])(:[0-5][0-9])?)");
            Matcher matcher =  norlogesPattern.matcher( inputMessage.getMessage() );

            while( matcher.find()){
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                String today = df.format(new Date());
                String norlogeParent = today + matcher.group(0);
                norlogeParent = norlogeParent.replace( ":","");
                Log.d( TAG, "This new post contains norloge! : " + matcher.group(0) + " have to query for " + norlogeParent + " in table" );

                String parentIdQuery  = "select id from messages where time = '" + norlogeParent + "'";
                Cursor cursor2 = database.rawQuery( parentIdQuery , null);

                if( cursor2.getCount() == 0){
                    Log.d( TAG, "This new post contains norloge with no parent :( ipot?");
                }else{
                    //Todo: peut renvoyer plusieurs, pour l'instant on prend uniquement le premier*/
                    cursor2.moveToFirst();
                    Log.d( TAG, "This post is answer to :" + cursor2.getLong( 0));
                    String insertQuery = "INSERT INTO " + SqliteHelper.ANSWERS_TABLE + " VALUES ( " + cursor2.getLong( 0) + "," + insertId + ");";
                    Log.d( TAG, "INSERT--> " + insertQuery);
                    database.execSQL( insertQuery );
                }
                cursor2.close();
            }





            return insertId;
        }
        cursor.close();
        return -1;
    }





        private void notifyOnNewPost( Missive inputMessage, String reason){
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
    public List<Missive> getAllMessages( String inputBoard){
       List<Missive> messages = new ArrayList<Missive>();
       Cursor cursor = database.query( SqliteHelper.MESSAGES_TABLE, allColumns, SqliteHelper.COLUMN_BOARD_ID + " == '" + inputBoard +"'", null, null, null, SqliteHelper.COLUMN_TIME, null);

       String postsAsString = PreferenceManager.getDefaultSharedPreferences( context).getString( "pref_max_post_displayed", "100");
       int maxPosts = Integer.parseInt( postsAsString);

       if( cursor.getCount() < maxPosts )
            cursor.moveToFirst();
        else
           cursor.moveToPosition( cursor.getCount() - maxPosts );

        while (!cursor.isAfterLast()) {
            Missive mess = new Missive();
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
