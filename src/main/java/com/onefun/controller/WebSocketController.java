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
    @ApiOperation(value="通过roomToken销毁房间", notes="通过roomToken销毁房间")
    @ApiImplicitParam(name = "roomToken", paramType="query", value = "房间号", dataType="String", required = true)
    public void destroyRoom(@RequestParam String roomToken) {
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Map<String,Player> pM  = RoomWebSocket.getPlayingMap();
        Room room = rM.remove(roomToken);
        room.sendMessage("5","房间销毁");
        pM.remove(room.getPlayer1().getLoginId());
        pM.remove(room.getPlayer2().getLoginId());
        //方便垃圾回收
        room = null;
    }

    @PostMapping(value = "/actionPalyer")
    @ApiOperation(value="操作玩家", notes="操作玩家")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roomToken",  value = "房间号",paramType="query", dataType="String", required = true),
            @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    })
    public void actionPalyer(@RequestParam String roomToken ,@RequestParam String loginId) {
    }

    @PostMapping(value = "/challengePalyer")
    @ApiOperation(value="约战玩家", notes="约战玩家")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roomToken",  value = "房间号",paramType="query", dataType="String"),
            @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "query    ",dataType = "string", required = true)
    })
    public void challengePalyer(@RequestParam String roomToken ,@RequestParam String loginId) {
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Map<String,Player> pM  = RoomWebSocket.getOnlinePlayerMap();
        Player p = pM.get(loginId);
        p.setRoomToken(roomToken);
        if("null".equals(roomToken)){//发送邀请。创建房间
            //房间token
            roomToken = UUID.randomUUID().toString();  //转化为String对象
            roomToken = roomToken.replace("-", ""); //因为UUID本身为32位只是生成时多了“-”，所以将它们去点就可
            logger.debug(roomToken);
            p.setRoomToken(roomToken);
            Room a = Room.newRoom().setPlayer1(p);
            rM.put(roomToken,a);
            SocketResult sr = SocketResult.newSocketResult().setState("2").setRoomToken(roomToken).setLoginId(loginId).setData("匹配中");
            p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
        }else{//接受邀请，进入房间
            Room a = rM.get(roomToken);
            if(a!=null){
                a.setPlayer2(p);
                a.sendMessage("3","匹配成功");
            }else{
                SocketResult sr = SocketResult.newSocketResult().setState("5").setRoomToken(roomToken).setLoginId(loginId).setData("房间已销毁。");
                p.getSession().getAsyncRemote().sendText(JSON.toJSONString(sr));
            }
        }
    }

    @PostMapping(value = "/challenge")
    @ApiOperation(value="随机匹配", notes="随机匹配")
    @ApiImplicitParam(name = "loginId",value = "用户id",paramType = "form",dataType = "string", required = true)
    public void challenge(@RequestParam String loginId) {
        Map<String,Player> wM  = RoomWebSocket.getWaitList();
        Map<String,Player> pM  = RoomWebSocket.getOnlinePlayerMap();
        Map<String,Player> playingM  = RoomWebSocket.getPlayingMap();
        Player p = pM.get(loginId);
        //进入等待队列，自动匹配
        SocketResult sr = SocketResult.newSocketResult().setState("2").setData("自动匹配。。。");
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















    /*
    @RequestMapping(value = "/admin")
    public String admin(Model model) {
        int num = socketServer.getOnlineNum();
        String str = socketServer.getOnlineUsers();
        model.addAttribute("num",num);
        model.addAttribute("users",str);
        return "admin";
    }

    *//**
     * 个人信息推送
     * @return
     *//*
    @RequestMapping("sendmsg")
    @ResponseBody
    public String sendmsg(String msg,String username){
        //第一个参数 :msg 发送的信息内容
        //第二个参数为用户长连接传的用户人数
        String [] persons = username.split(",");
        SocketServer.SendMany(msg,persons);
        return "success";
    }

    *//**
     * 推送给所有在线用户
     * @return
     *//*
    @RequestMapping("sendAll")
    @ResponseBody
    public String sendAll(String msg){
        SocketServer.sendAll(msg);
        return "success";
    }

    *//**
     * 获取当前在线用户
     * @return
     *//*
    @RequestMapping("webstatus")
    public String webstatus(){
        //当前用户个数
        int count = SocketServer.getOnlineNum();
        //当年用户的username
        SocketServer.getOnlineUsers();
        return "tongji";
    }*/
}