package fr.gabuzomeu.canadeche;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ReorderBoardsActivity extends ListActivity {

    private Context context;
    private ArrayList enabledBoardsNameList = new ArrayList();
    ArrayAdapter<String> drawerBoardsListAdapter;
    SharedPreferences prefs;
    private IconicAdapter adapter=null;
    private boolean debug = false;
    private static String TAG = "CanadecheReorderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences( getBaseContext());
        context = getApplicationContext();
        debug = prefs.getBoolean("pref_debug", false);

        setContentView(R.layout.reorder_activity_layout);



        int size = prefs.getInt( "boards_enabled_size", 0);
        for( int i =0; i < size; i++){
            Log.d( TAG, "Read Board["+i+"] >> " + prefs.getString( "boards_enabled_" + i, "empty"));
            enabledBoardsNameList.add( prefs.getString( "boards_enabled_" + i, "empty"));

        }


        TouchListView tlv=(TouchListView)getListView();
        adapter=new IconicAdapter();
        setListAdapter(adapter);

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);

    }

    @Override
    public void onDestroy(){
        if( debug)
            Log.d( TAG, "Leave reorder settings");

        int size = adapter.getCount();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt( "boards_enabled_size", size);
        editor.putLong( "boards_enabled_reorder_timestamp", System.currentTimeMillis());

        for (int i = 0; i < size; i++){
            editor.putString( "boards_enabled_" + i, adapter.getItem( i) );
            Log.d( TAG, "Board["+i+"] >> " + adapter.getItem( i));
        }
        editor.commit();

        super.onDestroy();
    }





    private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            String item=adapter.getItem(from);

            adapter.remove(item);
            adapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove=new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            adapter.remove(adapter.getItem(which));
        }
    };

    class IconicAdapter extends ArrayAdapter<String> {
        IconicAdapter() {
            super(ReorderBoardsActivity.this, R.layout.boardrow, enabledBoardsNameList);
        }

        public View getView(int position, View convertView,
                            ViewGroup parent) {
            View row=convertView;

            if (row==null) {
                LayoutInflater inflater=getLayoutInflater();

                row=inflater.inflate(R.layout.boardrow, parent, false);
            }

            TextView label=(TextView)row.findViewById(R.id.label);

            label.setText((String) enabledBoardsNameList.get(position));

            return(row);
        }
    }


    
}
