package fr.gabuzomeu.canadeche;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class BoardParser extends DefaultHandler {


    private boolean in_time = false;
    private boolean in_id = false;
    private boolean in_mytag = false;
    private boolean in_info = false;
    private boolean in_message = false;
    private boolean in_a = false;
    private boolean in_login = false;
    private boolean in_post = false;
    private boolean in_board = false;
    private boolean in_site = false;
    private boolean in_timezone = false;


    private String boardName;
    private Context context;
    private boolean debug = false;

    private boolean isTagEncoded = true;

    private String TAG = "CanadecheBoardParser";
    private Missive message;

    ArrayList<Missive> messages;
    private SharedPreferences prefs;


    public BoardParser( Context ctx, String _boardName){
        boardName = _boardName;
        prefs = PreferenceManager.getDefaultSharedPreferences( ctx);
        isTagEncoded = prefs.getBoolean( "boardconfig_"+ _boardName + "_checkbox_boardtagsencoded", true);
        debug = prefs.getBoolean("pref_debug", false);
        if( debug)
            Log.d( TAG, "isTagEncoded  " + _boardName + " = " + isTagEncoded);
    }

    public ArrayList<Missive> getMessages() {
        return messages;
    }

    @Override
    public void startDocument() throws SAXException {
        if( debug)
            Log.d( TAG, "Parsing started");
        messages=new ArrayList();
    }

    @Override
    public void endDocument() throws SAXException {
        if( debug)
        Log.d( TAG, "Parsing ended " + messages.size() +  " messages parsed" );
    }


    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {

        if( localName.equals("time")){

            //this.in_time=true;
        }else if ( localName.equals("id")){
            this.in_id=true;
        }else if ( localName.equals("info")){
            this.in_info=true;
        }else if ( localName.equals("message")){
            this.in_message=true;
        }else if ( localName.equals("login")){
            this.in_login=true;
        }else if ( localName.equals("post")){
            message=new Missive();
            message.setMessage( "");
            message.setId( Integer.parseInt( atts.getValue("id")));
            message.setTime( Long.parseLong( atts.getValue("time")));
            this.in_post=true;
        }else if ( localName.equals("a") && this.in_message == true){
            message.setMessage( message.getMessage() + " " + atts.getValue("href") + " ");
            this.in_a=true;
        }else if ( localName.equals("board")){
            this.in_board=true;
        }else if ( localName.equals("timezone")){
            this.in_timezone=true;
        }

    }


    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        if( localName.equals("time")){
            this.in_time=false;
        }else if ( localName.equals("id")){
            this.in_id=false;
        }else if ( localName.equals("info")){
            this.in_info=false;
        }else if ( localName.equals("message")){
            this.in_message=false;
        }else if ( localName.equals("login")){
            this.in_login=false;
        }else if ( localName.equals("post")){
            messages.add( message );
        }else if ( localName.equals("board")){
            this.in_board=false;
        }else if ( localName.equals("timezone")){
            this.in_timezone=false;
        }else if ( localName.equals("a")){
            this.in_a=false;
    }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String cdata = new String(ch, start, length);

        if( this.in_time){
            message.setTime( Integer.parseInt( cdata ));
            this.in_time=false;
        }else if( this.in_id ){
            message.setId( Integer.parseInt( cdata ));
            this.in_id=false;
        }else if( this.in_info ){
            message.setInfo( cdata );
            this.in_info=false;
        }else if( this.in_message ){
            if( !this.in_a){
                if( message.getMessage() != null){
                    message.setMessage( message.getMessage() + cdata );
                }
                else
                    message.setMessage( cdata);
            }
        }else if( this.in_login ){
            message.setLogin( cdata );
        }else if( this.in_board ){
            this.in_board=false;
        }

    }





}
