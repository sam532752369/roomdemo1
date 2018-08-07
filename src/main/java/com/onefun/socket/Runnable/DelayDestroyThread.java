package com.onefun.socket.Runnable;


import com.onefun.socket.Player;
import com.onefun.socket.Room;
import com.onefun.socket.RoomWebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Administrator on 2018/8/3.
 */
public class DelayDestroyThread implements Runnable{
    //房间队列
    private static Map<String,Room> roomMap = null;
    //正在对战玩家队列 loginid-player
    private static Map<String,Player> playingMap = null;
    private String roomToken;
    private long delayTime = 5000;//默认5秒

    public void setRoomToken(String roomToken) {
        this.roomToken = roomToken;
    }

    public void setDelayTime(long delayTime) {

        this.delayTime = delayTime;
    }

    public Map<String, Room> getRoomMap() {
        return roomMap;
    }

    public void setRoomMap(Map<String, Room> roomMap) {
        this.roomMap = roomMap;
    }

    public  Map<String, Player> getPlayingMap() {
        return playingMap;
    }

    public  void setPlayingMap(Map<String, Player> playingMap) {
        DelayDestroyThread.playingMap = playingMap;
    }

    @Override
    public void run() {
            try {
                System.out.println("延迟5秒");
                System.out.println(roomMap);
                System.out.println(roomToken);
                Thread.sleep(delayTime);
                //销毁房间，去除
                synchronized(roomMap) {
                    Room room = roomMap.remove(roomToken);
                    playingMap.remove(room.getPlayer1().getLoginId());
                    playingMap.remove(room.getPlayer2().getLoginId());
                    room.sendMessage("1","房间销毁");
                }
                System.out.println("房间销毁");
            }  catch (InterruptedException e) {
                System.out.println("在沉睡中被停止, 进入catch");
            }catch (Exception e) {
                e.printStackTrace();
            }
    }
}
