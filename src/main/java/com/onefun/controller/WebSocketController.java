package com.onefun.controller;

/**
 * Created by Administrator on 2018/8/4.
 */

import com.alibaba.fastjson.JSON;
import com.onefun.socket.Player;
import com.onefun.socket.Room;
import com.onefun.socket.RoomWebSocket;
import com.onefun.socket.Runnable.MatchingThread;
import com.onefun.socket.SocketResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * websocket
 * 消息推送(个人和广播)
 */
@RestController
@Api(value="/wsc", description="wsc 控制器")
@RequestMapping("/wsc")
public class WebSocketController {
    private final static Logger logger = LoggerFactory.getLogger(RoomWebSocket.class);
    @Autowired
    private RoomWebSocket rws ;

    @GetMapping(value = "/index")
    public String idnex() {
        System.out.println("=====================");
        return "index";
    }

    @PostMapping(value = "/destroyRoom")
    @ApiOperation(value="通过roomToken销毁房间/退出游戏", notes="通过roomToken销毁房间")
    @ApiImplicitParam(name = "roomToken", paramType="from", value = "房间号", dataType="String", required = true)
    public void destroyRoom(String roomToken) {
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Map<String,Player> pM  = RoomWebSocket.getPlayingMap();
        Room room = rM.remove(roomToken);
        if(room!=null) {//防止同时调用报错
            logger.debug("房间销毁");
            room.sendMessage("5", "房间销毁，退出游戏");
            if(room.getPlayer1()!=null){
                pM.remove(room.getPlayer1().getLoginId());
            }
            if(room.getPlayer2()!=null){
                pM.remove(room.getPlayer2().getLoginId());
            }
        }
        //方便垃圾回收
        room = null;
    }

    @PostMapping(value = "/actionPalyer")
    @ApiOperation(value="操作玩家", notes="操作玩家")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roomToken",  value = "房间号",paramType="from", dataType="String", required = true),
            @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    })
    public void actionPalyer(@RequestParam String roomToken ,@RequestParam String loginId) {
    }

    @PostMapping(value = "/challengePalyer")
    @ApiOperation(value="约战玩家", notes="约战玩家")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roomToken",  value = "房间号",paramType="from", dataType="String"),
            @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "String", required = true)
    })
    public void challengePalyer( String roomToken , String loginId) {
        logger.debug(roomToken+"====="+loginId);
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Map<String,Player> pM  = RoomWebSocket.getOnlinePlayerMap();
        Map<String,Player> playingM  = RoomWebSocket.getPlayingMap();
        Player p = pM.get(loginId);
        p.setRoomToken(roomToken);
        if(roomToken==null||"null".equals(roomToken)){//发送邀请。创建房间
            //房间token
            roomToken = UUID.randomUUID().toString();  //转化为String对象
            roomToken = roomToken.replace("-", ""); //因为UUID本身为32位只是生成时多了“-”，所以将它们去点就可
            logger.debug(roomToken);
            p.setRoomToken(roomToken);
            Room a = Room.newRoom().setPlayer1(p);
            rM.put(roomToken,a);
            SocketResult sr = SocketResult.newSocketResult().setState("2").setRoomToken(roomToken).setLoginId(loginId).setData("匹配中");
            p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
            playingM.put(loginId,p);
        }else{//接受邀请，进入房间
            Room a = rM.get(roomToken);
            if(a!=null){
                if(a.getPlayer2()==null){
                    a.setPlayer2(p);
                    a.sendMessage("3","匹配成功");
                    playingM.put(loginId,p);
                }else{
                    SocketResult sr = SocketResult.newSocketResult().setState("6").setRoomToken(roomToken).setLoginId(loginId).setData("房间已满人。");
                    p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
                }

            }else{
                SocketResult sr = SocketResult.newSocketResult().setState("5").setRoomToken(roomToken).setLoginId(loginId).setData("房间已销毁。");
                p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
            }
        }
    }

    @PostMapping(value = "/challenge")
    @ApiOperation(value="随机匹配", notes="随机匹配")
    @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    public void challenge(String loginId) {
        Map<String,Player> wM  = RoomWebSocket.getWaitList();
        Map<String,Player> pM  = RoomWebSocket.getOnlinePlayerMap();
        Map<String,Player> playingM  = RoomWebSocket.getPlayingMap();
        Player p = pM.get(loginId);
        //进入等待队列，自动匹配
        SocketResult sr = SocketResult.newSocketResult().setState("2").setLoginId(loginId).setData("自动匹配。。。");
        p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
        wM.put(loginId,p);
        logger.debug(JSON.toJSONString(sr));
        //调用匹配线程
        //另起线程判断 延迟断线时间，让玩家重连
        MatchingThread mt =new MatchingThread();
        mt.setRoomMap(RoomWebSocket.getRoomMap());
        mt.setWaitList(wM);
        mt.setPlayingMap(playingM);
        new Thread(mt).start();
    }

    @PostMapping(value = "/cancleChallenge")
    @ApiOperation(value="取消匹配", notes="取消匹配")
    @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    public void cancleChallenge(String loginId) {
        Map<String,Player> wM  = RoomWebSocket.getWaitList();
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Player p = wM.remove(loginId);
        if(p==null){
            Map<String,Player> onlineM  = RoomWebSocket.getOnlinePlayerMap();
            p = onlineM.get(loginId);
        }
        logger.debug(p.getRoomToken()==null?"":p.getRoomToken());
        Room room = rM.remove(p.getRoomToken()==null?"":p.getRoomToken());
        SocketResult sr = SocketResult.newSocketResult().setState("7").setLoginId(loginId).setData("取消匹配");
        if(room!=null){
            room.sendMessage(sr);
        }else {
            p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
        }
    }

    @PostMapping(value = "/exitRoom")
    @ApiOperation(value="退出房间", notes="退出房间")
    @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    public void exitRoom(String loginId) {
        Map<String,Player> playingM  = RoomWebSocket.getPlayingMap();
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Player p = playingM.remove(loginId);
        Room room = rM.get(p.getRoomToken());

    }
}