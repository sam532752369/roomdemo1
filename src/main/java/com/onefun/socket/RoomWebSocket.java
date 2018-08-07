package com.onefun.socket;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onefun.socket.Runnable.DelayDestroyThread;
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
    private static Map<String,Player> sessionPool = new ConcurrentHashMap<String,Player>();
    //保存 sessionid - loginid
    private static Map<String,String> sessionIds = new ConcurrentHashMap<String,String>();
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

    /**
     * 连接建立成功调用的方法
     * 添加进需要匹配的set
     * */
    @OnOpen
    public void onOpen(Session session,@PathParam(value="loginId")String loginId) {
        System.out.println("登陆者："+loginId);
        this.session = session;
        if(sessionPool.get(loginId)!=null){
//            System.out.println("已存在");
            sessionPool.put(loginId,Player.newPlayer().setLoginId(loginId).setSession(session));
            waitList.remove(loginId);
        }else{
//            System.out.println("新增");
            sessionPool.put(loginId, Player.newPlayer().setLoginId(loginId).setSession(session));
            sessionIds.put(session.getId(), loginId);
        }
        //判断是否正在对战
        if(playingMap.get(loginId)!=null){
            System.out.println(loginId+"玩家正在游戏中");
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
                 sr = SocketResult.newSocketResult().setState("5").setData("房间已销毁-"+roomToken);
            }else{
                sr = SocketResult.newSocketResult().setState("1").setData("重连成功").setRoomToken(r.getRoomToken());
            }

            r.sendMessage(sr);
        }else{
            try {
                //进入等待队列，自动匹配
                if(waitList.size()%2==0){
                    SocketResult sr = SocketResult.newSocketResult().setState("2").setData("自动匹配。。。");
                    sendMessage(this.session,JSON.toJSONString(sr));
                    Player p = Player.newPlayer().setSession(session).setLoginId(loginId);
                    waitList.put(loginId,p);
                    System.out.println(JSON.toJSONString(sr));
                }else{
                    if(true){
//                        System.out.println("waitList长度："+waitList.size());
//                        System.out.println("roomList长度："+roomMap.size());
                        Player p1 = Player.newPlayer().setSession(this.session).setLoginId(loginId);
                        //根据匹配法则获取对手
                        Player p2;
                        //从等待队列中获取玩家
                        synchronized(waitList){
                            String pipeikey = (String)waitList.keySet().toArray()[0];
                            p2 = waitList.get(pipeikey);
                            waitList.remove(pipeikey);
                        }
                        Room a = new Room(p1,p2);

                        //房间token
                        String roomToken = UUID.randomUUID().toString();  //转化为String对象
                        System.out.println(roomToken);   //打印UUID
                        roomToken = roomToken.replace("-", ""); //因为UUID本身为32位只是生成时多了“-”，所以将它们去点就可
                        System.out.println(roomToken);
                        roomMap.put(roomToken,a);
//                        this.roomToken = roomToken;
                        p1.setRoomToken(roomToken);
                        p2.setRoomToken(roomToken);
                        a.setRoomToken(roomToken);
                        //放进正在游戏中map
                        playingMap.put(p1.getLoginId(),p1);
                        playingMap.put(p2.getLoginId(),p2);

//                        SocketResult sr = new SocketResult("1",roomToken,"匹配成功");
                        a.sendMessage("3","匹配成功");
//                        this.session.getAsyncRemote().sendText(JSON.toJSONString(sr));
//                        .getAsyncRemote().sendText(JSON.toJSONString(sr));
//                        System.out.println("匹配成功");

                        return;
                    }
                }

            } catch (IOException e) {
                System.out.println("IO异常");
                e.printStackTrace();
                logger.error("发送消息失败:"+e.getMessage());
            }
        }



    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
          System.out.println("关闭连接");
          waitList.remove(loginId);
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
        //解析出错
        SocketResult sr = null;
        try {
            sr = om.readValue(message,SocketResult.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(sr.getLoginId()!=null){
            this.loginId = sr.getLoginId();
        }
        if(sr.getRoomToken()!=null){
            this.roomToken = sr.getRoomToken();
            Room a = roomMap.get(sr.getRoomToken());
            if(a!=null){
                SocketResult sr1 = SocketResult.newSocketResult().setState("1").setRoomToken(roomToken).setData(sr.getData());
                a.sendMessage(sr1);
            }else{
                SocketResult sr1 = SocketResult.newSocketResult().setState("1").setRoomToken(roomToken).setData("房间已销毁，连接已断开，请重新连接");
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
        for (String s : sessionPool.keySet()) {
                sessionPool.get(s).getSession().getAsyncRemote().sendText(message);//异步
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