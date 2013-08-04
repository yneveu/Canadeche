package fr.gabuzomeu.canadeche;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;


public class MessageAdapter extends ArrayAdapter<Message> {

    private Context context;
    private List<Message> messagesList;
    String boardName;
    private SharedPreferences prefs;
    boolean debug = false;
    EditText ed;
    public static String TAG = "CanadecheMessageAdapter";


    public MessageAdapter(Context context, int viewId, List<Message> messagesList, String _boardName, EditText _ed ) {
        super( context, viewId, messagesList);
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences( context);
        debug = prefs.getBoolean("pref_debug", false);
        this.messagesList = messagesList;
        boardName = _boardName;
        ed = _ed;

    }

    public int getCount() {
        return messagesList.size();
    }

    public Message getItem(int position) {
        return messagesList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

/**todo: Utiliser les viewHolder pour les perfs http://www.vogella.com/articles/AndroidListView/article.html#adapterperformance */
    public View getView(int position, View convertView, ViewGroup parent) {

        int nbMessages = messagesList.size();
        //final Message message = messagesList.get( nbMessages - ( position + 1));
        final Message message = messagesList.get(  position);
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
                ed.setText( hour + ":" + minutes +":" + seconds + " ");
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
        Linkify.addLinks( post, Linkify.ALL);

        //Totoz
        Pattern pattern = Pattern.compile("\\[:(.*)\\]");
        String scheme = "totoz://";
        Linkify.addLinks( post, pattern, scheme);


        return v;


    }

}
