package com.onefun.controller;

/**
 * Created by Administrator on 2018/8/4.
 */

import com.onefun.socket.Player;
import com.onefun.socket.Room;
import com.onefun.socket.RoomWebSocket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.Map;

/**
 * websocket
 * 消息推送(个人和广播)
 */
@RestController
@Api(value="/wsc", description="wsc 控制器")
@RequestMapping("/wsc")
public class WebSocketController {
    @Autowired
    private RoomWebSocket rws ;

    /*@GetMapping("/getMsgById")
    @ApiOperation(value="通过id获取Msg", notes="通过id获取Msg")
    @ApiImplicitParam(name = "id", paramType="query", value = "MsgID", dataType="integer", required = true)
    public JSONResult<Msg> getMsgById(Integer id) throws Exception{
        JSONResult<Msg> resJson = new JSONResult<>();
        Msg param= msgService.selectOneByObj(id);
        resJson.setData(param);
        resJson.setStatus(0);
        return resJson;
    }*/
    @GetMapping(value = "/index")
    public String idnex() {
        System.out.println("=====================");
        return "index";
    }

    @PostMapping(value = "/delayRoom")
    @ApiOperation(value="通过roomToken销毁房间", notes="通过roomToken销毁房间")
    @ApiImplicitParam(name = "roomToken", paramType="query", value = "roomToken", dataType="String", required = true)
    public void delayRoom(@RequestParam String roomToken) {
        Map<String,Room> rM  = RoomWebSocket.getRoomMap();
        Map<String,Player> pM  = RoomWebSocket.getPlayingMap();
        Room room = rM.remove(roomToken);
        room.sendMessage("1","房间销毁");
        pM.remove(room.getPlayer1().getLoginId());
        pM.remove(room.getPlayer2().getLoginId());
//        rM.get(roomToken).destroyRoom();
//        return "admin";
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