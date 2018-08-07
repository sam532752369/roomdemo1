package com.onefun.socket.Runnable;


import com.onefun.socket.Player;
import com.onefun.socket.Room;
import com.onefun.socket.RoomWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2018/8/3.
 */
public class MatchingThread implements Runnable{
    private final static Logger logger = LoggerFactory.getLogger(RoomWebSocket.class);
    //房间队列
    private Map<String,Room> roomMap = null;
    //正在对战玩家队列 loginid-player
    private Map<String,Player> playingMap = null;
    //等待队列 loginid-Player
    private Map<String,Player> waitList = null;

    public Map<String, Player> getWaitList() {
        return waitList;
    }
    public void setWaitList(Map<String, Player> waitList) {
        this.waitList = waitList;
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
        this.playingMap = playingMap;
    }

    @Override
    public void run() {
        logger.debug("调用匹配线程");
        boolean isMatch = false;
        Player p1 = null;
        Player p2 = null;
        synchronized (waitList) {
            if(waitList.size()>1) {
                logger.debug("进入匹配");
                //获取等待着的loginId集合
                Set waitKey = waitList.keySet();
                String p1key = (String) waitKey.toArray()[0];
                String p2key = (String) waitKey.toArray()[1];
                logger.debug(p1key + "======" + p2key);
                p1 = waitList.get(p1key);
                p2 = waitList.get(p2key);
                waitList.remove(p1key);
                waitList.remove(p2key);
                isMatch = true;
            }
        }
        //如果匹配出了玩家，则生成房间
        if(isMatch) {
            Room a = new Room(p1,p2);
            //房间token
            String roomToken = UUID.randomUUID().toString();  //转化为String对象
            roomToken = roomToken.replace("-", ""); //因为UUID本身为32位只是生成时多了“-”，所以将它们去点就可
            logger.debug(roomToken);
            roomMap.put(roomToken,a);
            p1.setRoomToken(roomToken);
            p2.setRoomToken(roomToken);
            a.setRoomToken(roomToken);
            //放进正在游戏中map
            playingMap.put(p1.getLoginId(),p1);
            playingMap.put(p2.getLoginId(),p2);
            a.sendMessage("3","匹配成功");
            logger.debug("生成房间："+roomToken);
        }
    }
}
