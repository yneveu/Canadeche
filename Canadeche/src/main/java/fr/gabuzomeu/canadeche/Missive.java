package fr.gabuzomeu.canadeche;

import java.util.ArrayList;

public class Missive implements Comparable {

    //private int boardId;
    private String board;
    private long time;
    private int id;
    private String info;
    private String message;
    private String login;
    private String backgroundColor;

    public ArrayList<Long> getAnswersIds() {
        return answersIds;
    }

    public void setAnswersIds(ArrayList<Long> answersIds) {
        this.answersIds = answersIds;
    }

    public ArrayList<Long> getRespondToIds() {
        return respondToIds;
    }

    public void setRespondToIds(ArrayList<Long> respondToIds) {
        this.respondToIds = respondToIds;
    }

    private ArrayList<Long> answersIds;
    private ArrayList<Long> respondToIds;

    private boolean inFilter = false;



    public boolean getInFilter(){
        return inFilter;
    }

    public void setInFilter( boolean  mBool){
        inFilter = mBool;
    }


    public String getBoard() {
        return board;
    }
    public void setBoard(String board) {
        this.board = board;
    }

    public long getTime() {
        return time;
    }
    public void setTime(long l) {
        this.time = l;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String toString() {
		return "Message [ board=" + board + " id=" + id + " time=" + time + " message= " + message + "]";
    }
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    public String getBackgroundColor() {
        return backgroundColor;
    }
    public int compareTo(Object another) {
        if ( ((Missive)another).getTime() > this.getTime() )
            return 1;
        else if (((Missive)another).getTime() < this.getTime())
            return -1;
        else
            return 0;

    }

}
