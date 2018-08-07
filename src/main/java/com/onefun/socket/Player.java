package com.onefun.socket;

import javax.websocket.Session;

/**
 * Created by Administrator on 2018/8/4.
 */
public class Player {
    private Session session;
    private String roomToken;
    private String loginId;

    private Player() {
    }

    public static Player newPlayer(){
        return new Player();
    }




    public Session getSession() {
        return session;
    }

    public Player setSession(Session session) {
        this.session = session;
        return this;
    }

    public String getRoomToken() {
        return roomToken;
    }

    public Player setRoomToken(String roomToken) {
        this.roomToken = roomToken;
        return this;
    }

    public String getLoginId() {
        return loginId;
    }

    public Player setLoginId(String loginId) {
        this.loginId = loginId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        if (session != null ? !session.equals(player.session) : player.session != null) return false;
        if (roomToken != null ? !roomToken.equals(player.roomToken) : player.roomToken != null) return false;
        return loginId != null ? loginId.equals(player.loginId) : player.loginId == null;

    }

    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + (roomToken != null ? roomToken.hashCode() : 0);
        result = 31 * result + (loginId != null ? loginId.hashCode() : 0);
        return result;
    }
}
