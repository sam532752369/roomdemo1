package com.onefun.socket;

/**
 * Created by Administrator on 2018/8/2.
 */
public class SocketResult {
    private String loginId;
    private String roomToken;
    private String state;
    private String message;
    private String data;
    public SocketResult(){
    }


    public static SocketResult newSocketResult(){
        return new SocketResult();
    }

    public String getRoomToken() {
        return roomToken;
    }

    public SocketResult setRoomToken(String roomToken) {
        this.roomToken = roomToken;
        return this;
    }

    public String getState() {
        return state;
    }

    public SocketResult setState(String state) {
        this.state = state;
        return this;
    }

    public String getLoginId() {
        return loginId;
    }

    public SocketResult setLoginId(String loginId) {
        this.loginId = loginId;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public SocketResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getData() {
        return data;
    }

    public SocketResult setData(String data) {
        this.data = data;
        return this;
    }
}
