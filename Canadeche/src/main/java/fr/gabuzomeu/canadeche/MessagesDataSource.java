package fr.gabuzomeu.canadeche;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
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


    public static final String COLUMN_BOARD_ID = "board_id";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_INFO = "info";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_POST_ID = "post_id";

    public MessagesDataSource(Context context) {
        dbHelper = new SqliteHelper( context);
        debug = PreferenceManager.getDefaultSharedPreferences(context).getBoolean( "pref_debug", false);
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
            return insertId;
        }
        cursor.close();
        return -1;
    }

/**TODO: limit par tribune*/
    public List<Message> getAllMessages( String inputBoard){
       List<Message> messages = new ArrayList<Message>();
       Cursor cursor = database.query( SqliteHelper.MESSAGES_TABLE, allColumns, SqliteHelper.COLUMN_BOARD_ID + " == '" + inputBoard +"'", null, null, null, SqliteHelper.COLUMN_TIME, null);

       if( cursor.getCount() < 50)
            cursor.moveToFirst();
        else
           cursor.moveToPosition( cursor.getCount() -49);

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
