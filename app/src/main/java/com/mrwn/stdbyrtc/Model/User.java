package com.mrwn.stdbyrtc.Model;

/**
 * Created by Marouane 14-09-2016
 */
public class User {
    private String userId;
    private String userName;

    public User(String userId,String userName) {
        this.userId = userId;
        this.userName = userName;
    }


    public String getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }


    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (!(o instanceof HistoryItem)) return false;
        HistoryItem cu = (HistoryItem)o;
        return this.userId.equals(((HistoryItem) o).getUserId());
    }

    @Override
    public int hashCode() {
        return this.getUserId().hashCode();
    }
}