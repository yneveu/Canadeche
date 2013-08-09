package fr.gabuzomeu.canadeche;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.terlici.dragndroplist.DragNDropAdapter;
import com.terlici.dragndroplist.DragNDropCursorAdapter;
import com.terlici.dragndroplist.DragNDropListView;
import com.terlici.dragndroplist.DragNDropSimpleAdapter;

import java.util.ArrayList;
import java.util.Set;

public class ReorderBoardsActivity extends Activity {

    private Context context;
    private ArrayList enabledBoardsNameList;
    ArrayAdapter<String> drawerBoardsListAdapter;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      /*  prefs = PreferenceManager.getDefaultSharedPreferences( getBaseContext());
        context = getApplicationContext();

        setContentView(R.layout.reorder_activity_layout);

        Set<String> set = prefs.getStringSet( "enabled_boards",null );

        enabledBoardsNameList = new ArrayList( set);

        ListView list = (DragNDropListView)findViewById(android.R.id.list);
        ArrayAdapter adapter = new ArrayAdapter<String>( getApplicationContext(), android.R.layout.simple_list_item_1, enabledBoardsNameList);


        list.setAdapter( adapter);*/

    }



    
}
