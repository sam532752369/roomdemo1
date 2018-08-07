package com.onefun.socket;


import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;

/**
 * Created by Administrator on 2018/8/2.
 */
public class Room {
    private String roomToken;
    private Player player1 = null;
    private Player player2 = null;
    private final static Logger logger = LoggerFactory.getLogger(RoomWebSocket.class);

    public String getRoomToken() {
        return roomToken;
    }

    public void setRoomToken(String roomToken) {
        this.roomToken = roomToken;
    }

    public Room(Player p1 ,Player p2){
        this.player1 = p1;
        this.player2 = p2;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void reconnectRoom(String login,Session session){
        if(player1.getLoginId().equals(login)){
            player1.setSession(session);
        }else if(player2.getLoginId().equals(login)){
            player2.setSession(session);
        }
    }

    /**
     * 房间内传输信息,自动构建返回集
     * */
    @SuppressWarnings(value = "all")
    public void sendMessage(String state , String message)  {
        if(player1!=null&&player2!=null){
            SocketResult sr = SocketResult.newSocketResult().setState(state).setRoomToken(roomToken).setData(message);
            if(player1.getSession().isOpen()){
                player1.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));//异步
            }
            if(player2.getSession().isOpen()){
                player2.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));//异步
            }
            logger.info("房间销毁");

//                try {
//
//                } catch (Exception e) {
////                   player2.getSession().getAsyncRemote().sendText(JSON.toJSONString(SocketResult.newSocketResult().setState("4").setRoomToken(roomToken).setData("对方正在丢失")));//异步
//                    logger.error("发送消息失败:"+e.getMessage());
//                }
//                try {
//                    player2.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));//异步
//                } catch (Exception e) {
////                    player1.getSession().getAsyncRemote().sendText(JSON.toJSONString(SocketResult.newSocketResult().setState("4").setRoomToken(roomToken).setData("对方正在丢失")));//异步
//                    logger.error("发送消息失败:"+e.getMessage());
//                }
        }
    }
    /**
     * 房间内传输信息，手动构建返回集
     * */
    @SuppressWarnings(value="all")
    public void sendMessage(SocketResult data)  {
        if(player1!=null&&player2!=null){
            try {
                player1.getSession().getAsyncRemote().sendText(JSON.toJSONString(data));//异步
            } catch (Exception e) {
                player2.getSession().getAsyncRemote().sendText(JSON.toJSONString(SocketResult.newSocketResult().setState("4").setRoomToken(roomToken).setData("对方正在丢失")));//异步
                e.printStackTrace();
                logger.error("发送消息失败:"+e.getMessage());
            }
            try {
                player2.getSession().getAsyncRemote().sendText(JSON.toJSONString(data));//异步
            } catch (Exception e) {
                player1.getSession().getAsyncRemote().sendText(JSON.toJSONString(SocketResult.newSocketResult().setState("4").setRoomToken(roomToken).setData("对方正在丢失")));//异步
                e.printStackTrace();
                logger.error("发送消息失败:"+e.getMessage());
            }
        }
    }

    public void destroyRoom(){

    }

}
