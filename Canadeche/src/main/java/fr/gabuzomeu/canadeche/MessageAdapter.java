package fr.gabuzomeu.canadeche;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MessageAdapter extends ArrayAdapter<Missive> implements Filterable {

    private Context context;
    private List<Missive> messagesList;
    //non filtered list
    private List<Missive> fullMessageList;
    String boardName;
    private SharedPreferences prefs;
    boolean debug = false;
    EditText ed;

    private NorlogeFilter norlogeFilter;

    public static String TAG = "CanadecheMessageAdapter";


    public MessageAdapter(Context context, int viewId, List<Missive> messagesList, String _boardName, EditText _ed ) {
        super( context, viewId, messagesList);
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences( context);
        debug = prefs.getBoolean("pref_debug", false);
        this.messagesList = messagesList;
        this.fullMessageList = messagesList;
        boardName = _boardName;
        ed = _ed;

    }

    public int getCount() {
        return messagesList.size();
    }

    public Missive getItem(int position) {
        return messagesList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

/**todo: Utiliser les viewHolder pour les perfs http://www.vogella.com/articles/AndroidListView/article.html#adapterperformance */
    public View getView(int position, View convertView, ViewGroup parent) {

        int nbMessages = messagesList.size();
        //final Message message = messagesList.get( nbMessages - ( position + 1));
        final Missive message = messagesList.get(  position);
        final ViewGroup vg = parent;
        View v = convertView;

        if( v == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate( R.layout.messagerow, null);
        }


        TextView login = (TextView)v.findViewById( R.id.login);
        String sLogin;
        if( message.getLogin() == null || message.getLogin().length() <= 0 ){
            sLogin = message.getInfo() ;
            login.setTypeface( Typeface.defaultFromStyle(Typeface.ITALIC),Typeface.ITALIC );
        }
        else
            sLogin =  message.getLogin() ;
        login.setTextColor( Color.RED);
        if( sLogin.length() >= 30){
                sLogin = sLogin.substring( 0, 29) + "...";
        }
        login.setText( sLogin);



        String sClock= String.valueOf( message.getTime());
        final String hour=sClock.substring( 8, 10);
        final String minutes=sClock.substring(10,12);
        final String seconds=sClock.substring( 12,14);
        String cNorloge =  hour + ":" + minutes + ":" + seconds;

        TextView norloge =  (TextView)v.findViewById( R.id.norloge);


        OnClickListener onNorlogeClickListener = new OnClickListener() {

            public void onClick(View v) {
                Log.i( TAG, "SEND BOARD ID : " + message.getBoard() +1 );
                TextView clicked = ( TextView)v;

                String stringToInsert = hour + ":" + minutes +":" + seconds + " ";

                int start = Math.max( ed.getSelectionStart(), 0);
                int end = Math.max( ed.getSelectionEnd(), 0);
                ed.getText().replace(Math.min(start, end), Math.max(start, end),
                stringToInsert, 0, stringToInsert.length());
            }
        };

        norloge.setOnClickListener( onNorlogeClickListener);
        norloge.setText( cNorloge);
        norloge.setTextColor( Color.BLUE);


        //Traitement du message
        TextView post =  (TextView)v.findViewById( R.id.post);
        String transformedMessage = message.getMessage();

        boolean isTagsEncoded = prefs.getBoolean( "boardconfig_" + boardName + "_checkbox_boardtagsencoded", false);

        transformedMessage= transformedMessage.replaceAll( "\">.*\\[.*\\]*.</a>" , " ");
        transformedMessage= transformedMessage.replaceAll( "<a href=\"" , " ");

        OnClickListener messageOnClickListener = new OnClickListener() {

            public void onClick(View v) {
               // Log.i( TAG, "CLICK: " + clicked.getText());
            }
        };



        post.setFocusable(true);
        post.setClickable( true);
        post.setLinksClickable( true);
        post.setOnClickListener( messageOnClickListener);

        Spanned span = Html.fromHtml(transformedMessage);
        post.setText( span);
        Linkify.addLinks( post, Linkify.WEB_URLS);

        //Totoz
        Pattern pattern = Pattern.compile("\\[:(.*)\\]");
        String scheme = "totoz://";
        Linkify.addLinks( post, pattern, scheme);

        //Norloges
        Pattern norlogesPattern = Pattern.compile("((?:1[0-2]|0[1-9])/(?:3[0-1]|[1-2][0-9]|0[1-9])#)?((?:2[0-3]|[0-1][0-9])):([0-5][0-9])(:[0-5][0-9])?([¹²³]|[:\\^][1-9]|[:\\^][1-9][0-9])?(@[A-Za-z0-9_]+)?");
        Matcher matcher =  norlogesPattern.matcher( post.getText() );
        scheme = "norloge://";
        Linkify.addLinks( post, norlogesPattern, scheme);

        if( message.getInFilter()){
            post.setBackgroundColor( Color.YELLOW);
            norloge.setBackgroundColor( Color.YELLOW);
            login.setBackgroundColor( Color.YELLOW);
            v.setBackgroundColor( Color.YELLOW);
            v.findViewById(R.id.infos).setBackgroundColor( Color.YELLOW);
            v.findViewById(R.id.message).setBackgroundColor( Color.YELLOW);


        }else{
            post.setBackgroundColor( Color.WHITE);
            norloge.setBackgroundColor( Color.WHITE);
            login.setBackgroundColor( Color.WHITE);
            v.setBackgroundColor( Color.WHITE);
            v.findViewById(R.id.infos).setBackgroundColor( Color.WHITE);
            v.findViewById(R.id.message).setBackgroundColor( Color.WHITE);

        }

        return v;


    }

    /**Norloges filtering*/
    @Override
    public Filter getFilter() {
        if ( norlogeFilter == null)
            norlogeFilter = new NorlogeFilter();
        else norlogeFilter.performFiltering( "");
        return norlogeFilter;
    }

    private class NorlogeFilter extends Filter {

        @Override
        protected FilterResults performFiltering( CharSequence constraint) {

            FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                // No filter implemented we return all the list
                for ( Missive m : fullMessageList) {
                    if( m.getInFilter()){
                        m.setInFilter( false);
                        Log.d( TAG, "Invalidate filter on " + m.getMessage());
                    }

                }

                results.values = fullMessageList;
                results.count = fullMessageList.size();

            }else{
                List<Missive> filteredMessagesList = new ArrayList<Missive>();
                for ( Missive m : fullMessageList) {

                    String sClock= String.valueOf( m.getTime());
                    final String hour=sClock.substring( 8, 10);
                    final String minutes=sClock.substring(10,12);
                    final String seconds=sClock.substring( 12,14);
                    String cNorloge =  hour + ":" + minutes + ":" + seconds;

                    filteredMessagesList.add( m);

                    if( m.getMessage().toLowerCase().contains( constraint.toString().toLowerCase()) ||
                        m.getInfo().toLowerCase().contains( constraint.toString().toLowerCase()) ||
                        m.getLogin() != null && m.getLogin().toLowerCase().contains( constraint.toString().toLowerCase()) ||
                        cNorloge.contains( constraint)
                        ){
                        m.setInFilter( true);

                    }
                }
                results.values = filteredMessagesList;
                results.count = filteredMessagesList.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.count == 0)
                notifyDataSetInvalidated();
            else {
                messagesList = (List<Missive>) results.values;
                notifyDataSetChanged();
            }
        }
    }



}
