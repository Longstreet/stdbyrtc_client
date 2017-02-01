package com.mrwn.stdbyrtc.Model;

/**
 * Created by Marouane on 14-09-2016
 */
public class Room {
    private String roomId;
    private String status;
    private String userName;

    public Room(String userId, String userName) {
        this.roomId = userId;
        this.userName = userName;
        this.status = "Offline";
    }

    public Room(String userId, String userName, String status) {
        this.roomId = userId;
        this.userName = userName;
        this.status = status;
    }


    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return userName;
    }

    public String getRoomStatus() {
        return status;
    }

    public void setRoomStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room cu = (Room) o;
        return this.roomId.equals(((Room) o).getRoomId());
    }

    @Override
    public int hashCode() {
        return this.getRoomId().hashCode();
    }
}