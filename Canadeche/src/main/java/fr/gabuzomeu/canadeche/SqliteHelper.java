package fr.gabuzomeu.canadeche;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by yann on 01/08/13.
 */
public class SqliteHelper extends SQLiteOpenHelper {

    private static String TAG = "SqliteHelper";
    boolean debug = false;

    public static final String COLUMN_BOARD_ID = "board_id";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_INFO = "info";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_POST_ID = "post_id";

    public static final String COLUMN_PARENT_POST_ID = "parent_post_id";
    public static final String COLUMN_CHILD_POST_ID = "child_post_id";


    public static final String MESSAGES_TABLE = "messages";
    public static final String ANSWERS_TABLE = "answers";

    private static final String CREATE_TABLE_MESSAGES = "CREATE TABLE IF NOT EXISTS " + MESSAGES_TABLE +"( "
            + COLUMN_BOARD_ID + " INTEGER , "
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_TIME + " LONG NOT NULL, "
            + COLUMN_INFO + " TEXT, "
            + COLUMN_MESSAGE + " TEXT NOT NULL, "
            + COLUMN_LOGIN + " TEXT, "
            + COLUMN_POST_ID + " INTEGER )";

    private static final String CREATE_TABLE_ANSWERS = " CREATE TABLE IF NOT EXISTS " + ANSWERS_TABLE + " ( "
            + COLUMN_PARENT_POST_ID + " LONG , "
            + COLUMN_CHILD_POST_ID + " LONG )";




    private static final String DATABASE_NAME = "messages.db";
    private static final int DATABASE_VERSION = 1;


    public SqliteHelper( Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        debug = PreferenceManager.getDefaultSharedPreferences( context).getBoolean( "pref_debug", false);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL( CREATE_TABLE_MESSAGES);
        sqLiteDatabase.execSQL( CREATE_TABLE_ANSWERS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        Log.d(TAG, "Upgrading database from version" + i + " to version " + i2);
    }
}
