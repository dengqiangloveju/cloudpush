package com.huangliang.cloudpushportal.service;

import com.huangliang.api.constants.Constants;
import com.huangliang.api.constants.RedisPrefix;
import com.huangliang.api.entity.WebsocketMessage;
import com.huangliang.api.entity.request.SendRequest;
import com.huangliang.api.util.RedisUtils;
import com.huangliang.cloudpushportal.service.messagedispatch.MessageDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 推送消息处理类
 */
@Slf4j
@Service
public class MessageService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MessageDispatchService messageDispatchService;

    public void execute(SendRequest request) {
        //查询redis中所有的websocket服务
        Set<String> set = redisTemplate.keys(RedisPrefix.PREFIX_SERVERCLIENTS + "*");
        if (CollectionUtils.isEmpty(set)) {
            return;
        }
        //<服务端地址,对应的客户端结果集>
        Map<String,List<String>> hostClientsMap = new HashMap<>(set.size());
        if (request.getSendToAll()) {
            //根据服务下的设备标识推送
            //2.消息分发给具体的websocket实例处理
            //serverKey => serverclients_10.9.217.160:9003
            for(String serverKey : set){
                messageDispatchService.send(serverKey,request);
            }
        }else{
            //根据参数中的客户端标识,找出所在的服务器，先对应的服务器发起推送
            List<String> requestClients = request.getTo();
            //批量查询
            List<Object> pipeResult = redisTemplate.executePipelined(RedisUtils.getClientHostByClientFromRedis(requestClients));
            for (int i=0;i<requestClients.size();i++) {
                //遍历list 依次存入推送消息
                //根据channelId找到对应的客户端对象所对应websocket服务的实例名
                String channelId = requestClients.get(i);
                //废弃单个查询的方式
                //String host = redisTemplate.opsForHash().get(RedisPrefix.PREFIX_CLIENT + channelId,"host")+"";
                Object hostObj = pipeResult.get(i);
                if (hostObj==null) {
                    log.info("不存在的客户端[{}]", channelId);
                    continue;
                }
                String host = hostObj.toString();
                if(hostClientsMap.containsKey(host)){
                    hostClientsMap.get(host).add(channelId);
                }else{
                    List<String> clients = new LinkedList<>();
                    clients.add(channelId);
                    hostClientsMap.put(host,clients);
                }
            }
            for(String hostItem : hostClientsMap.keySet()){
                messageDispatchService.send(hostItem,request);
            }
        }
    }
}
