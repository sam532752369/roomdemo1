package com.onefun.socket;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onefun.socket.Runnable.DelayDestroyThread;
import com.onefun.socket.Runnable.MatchingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.apache.coyote.http11.Constants.a;

@ServerEndpoint(value = "/websocket1/{loginId}")
@Component
public class RoomWebSocket {
    //静态变量会不会太多呢
	 private final static Logger logger = LoggerFactory.getLogger(RoomWebSocket.class);
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

//    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
//    private static CopyOnWriteArraySet<RoomWebSocket> webSocketSet = new CopyOnWriteArraySet<RoomWebSocket>();
    //在线人数 loginId-Player
    private static Map<String,Player> onlinePlayerMap = new ConcurrentHashMap<String,Player>();
//    //保存 sessionid - loginid
//    private static Map<String,String> sessionIds = new ConcurrentHashMap<String,String>();
    //等待队列 loginid-Player
    private static Map<String,Player> waitList = new ConcurrentHashMap<String,Player>();
    //房间队列
    private static Map<String,Room> roomMap = new ConcurrentHashMap<String,Room>();
    //正在对战玩家队列 loginid-player
    private static Map<String,Player> playingMap = new ConcurrentHashMap<String,Player>();
    //房间token
    private String roomToken ;
    //loginId
    private String loginId ;

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    public static Map<String, Room> getRoomMap() {
        return roomMap;
    }

    public static Map<String, Player> getPlayingMap() {
        return playingMap;
    }

    public static Map<String, Player> getOnlinePlayerMap() {
        return onlinePlayerMap;
    }

    public static Map<String, Player> getWaitList() {
        return waitList;
    }

    /**
     * 连接建立成功调用的方法
     * */
    @OnOpen
    public void onOpen(Session session,@PathParam(value="loginId")String loginId) {
        logger.debug("登陆者："+loginId);
        this.session = session;
        if(onlinePlayerMap.get(loginId)!=null){
//            System.out.println("已存在");
            onlinePlayerMap.put(loginId,Player.newPlayer().setLoginId(loginId).setSession(session));
            waitList.remove(loginId);
        }else{
//            System.out.println("新增");
            onlinePlayerMap.put(loginId, Player.newPlayer().setLoginId(loginId).setSession(session));
//            sessionIds.put(session.getId(), loginId);
        }
        //判断是否正在对战
        if(playingMap.get(loginId)!=null){
            logger.debug(loginId+"玩家正在游戏中");
            Room r;
            synchronized (roomMap){
                String roomToken = playingMap.get(loginId).getRoomToken();
                r = roomMap.get(roomToken);
                //如果房间不为空，重连回房间，中断销毁线程
                if(r!=null){
                    r.reconnectRoom(loginId,session);
                    ThreadGroup currentGroup =Thread.currentThread().getThreadGroup();
                    int noThreads = currentGroup.activeCount();
                    Thread[] lstThreads = new Thread[noThreads];
                    currentGroup.enumerate(lstThreads);
                    for (int i = 0; i < noThreads; i++){
                        if(lstThreads[i].getName().equals(roomToken)){
                            System.out.println("线程号：" + i + " = " + lstThreads[i].getName()+"被中断");
                            lstThreads[i].interrupt();
                        }
                }
                }else{
                    //如果房间为空，就应该是之前出错了
                }
            }
            SocketResult sr;
            if(r==null){
                 sr = SocketResult.newSocketResult().setState("5").setLoginId(loginId).setData("房间已销毁-"+roomToken);
            }else{
                sr = SocketResult.newSocketResult().setState("1").setLoginId(loginId).setData("重连成功").setRoomToken(r.getRoomToken());
            }

            r.sendMessage(sr);
        }else{
//            try {
//                //进入等待队列，自动匹配
//                SocketResult sr = SocketResult.newSocketResult().setState("2").setData("自动匹配。。。");
//                sendMessage(session,JSON.toJSONString(sr));
//                Player p = Player.newPlayer().setSession(session).setLoginId(loginId);
//                waitList.put(loginId,p);
//                logger.debug(JSON.toJSONString(sr));
//                //调用匹配线程
//                //另起线程判断 延迟断线时间，让玩家重连
//                MatchingThread mt =new MatchingThread();
//                mt.setRoomMap(roomMap);
//                mt.setWaitList(waitList);
//                mt.setPlayingMap(playingMap);
//                new Thread(mt).start();
//            } catch (IOException e) {
//                System.out.println("IO异常");
//                e.printStackTrace();
//                logger.error("发送消息失败:"+e.getMessage());
//            }
        }



    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
          System.out.println("关闭连接");
          waitList.remove(loginId);
          onlinePlayerMap.remove(loginId);
        if(playingMap.get(loginId)!=null){
            //另起线程判断 延迟断线时间，让玩家重连
            DelayDestroyThread ddt =new DelayDestroyThread();
            ddt.setRoomMap(roomMap);
            ddt.setRoomToken(playingMap.get(loginId).getRoomToken());
            ddt.setPlayingMap(playingMap);
            new Thread(ddt,playingMap.get(loginId).getRoomToken()).start();
        }

//
//        sessionPool.remove(sessionIds.get(session.getId()));
//        sessionIds.remove(session.getId());
////        webSocketSet.remove(this);  //从set中删除
//
//        Room r =roomMap.remove(this.roomToken);
////        if(r.getPlayer1()==this){
////            SocketResult sr = new SocketResult("1",null,"房间销毁");
////            r.getPlayer1().session.getAsyncRemote().sendText(JSON.toJSONString(sr));
////        }else{
////            SocketResult sr = new SocketResult("1",null,"房间销毁");
////            r.getPlayer2().session.getAsyncRemote().sendText(JSON.toJSONString(sr));
////        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        ObjectMapper om = new ObjectMapper();
        SocketResult sr = null;
        try {
            sr = om.readValue(message,SocketResult.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String loginId = null;
        if(sr.getLoginId()!=null){
            this.loginId = sr.getLoginId();
            loginId = sr.getLoginId();
        }
        if(sr.getRoomToken()!=null){
            this.roomToken = sr.getRoomToken();
            Room a = roomMap.get(sr.getRoomToken());
            if(a!=null){
                SocketResult sr1 = SocketResult.newSocketResult().setState("1").setRoomToken(roomToken).setLoginId(loginId).setData(sr.getData());
                a.sendMessage(sr1);
            }else{
                SocketResult sr1 = SocketResult.newSocketResult().setState("1").setRoomToken(roomToken).setLoginId(loginId).setData("房间已销毁，连接已断开，请重新连接");
                this.session.getAsyncRemote().sendText(JSON.toJSONString(sr1));
                try {
                    this.session.close();
                } catch (IOException e) {
                    logger.error("发送消息失败:"+e.getMessage());
                }
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }


    public static void sendMessage(Session session ,String message) throws IOException {
        //this.session.getBasicRemote().sendText(message);
        session.getAsyncRemote().sendText(message);//异步
    }


    /**
     * 群发自定义消息
     * */
    public static void sendInfo(String message){
        for (String s : onlinePlayerMap.keySet()) {
            onlinePlayerMap.get(s).getSession().getAsyncRemote().sendText(message);//异步
                continue;
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        RoomWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        RoomWebSocket.onlineCount--;
    }
}