package com.hao14293.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.pack.group.AddGroupMemberPack;
import com.hao14293.im.codec.pack.group.RemoveGroupMemberPack;
import com.hao14293.im.codec.pack.group.UpdateGroupMemberPack;
import com.hao14293.im.common.ClientType;
import com.hao14293.im.common.enums.Command.Command;
import com.hao14293.im.common.enums.Command.GroupEventCommand;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.service.group.model.req.GroupMemberDto;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Component
public class GroupMessageProducer {

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private ImGroupMemberService imGroupMemberService;

    public void producer(String userId, Command command, Object data, ClientInfo clientInfo){
        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");

        // 获取群内的所有群成员的id
        List<String> groupMemberId
                = imGroupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());

        // 加人的时候的TCP通知，只用告诉管理员和本人即可
        if(command.equals(GroupEventCommand.ADDED_MEMBER)){
            // 发送给管理员和被加入人本身
            List<GroupMemberDto> groupManager
                    = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            AddGroupMemberPack addGroupMemberPack = o.toJavaObject(AddGroupMemberPack.class);
            List<String> members = addGroupMemberPack.getMembers();
            // 发送给管理员
            for (GroupMemberDto groupMemberDto : groupManager) {
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode()
                        && groupMemberDto.getMemberId().equals(userId)){
                    messageProducer.sendToUserExceptClient(groupMemberDto.getMemberId(), command, data, clientInfo);
                }else{
                    messageProducer.sendToUser(groupMemberDto.getMemberId(), command, data, clientInfo.getAppId());
                }
            }

            // 发送给本人的其他端
            for (String member : members) {
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)){
                    messageProducer.sendToUserExceptClient(member, command, data, clientInfo);
                }else{
                    messageProducer.sendToUser(member, command, data, clientInfo.getAppId());
                }
            }
        }
        // 踢人出群的时候的tcp通知
        else if(command.equals(GroupEventCommand.DELETED_MEMBER)){
            // 获取
            RemoveGroupMemberPack pack = o.toJavaObject(RemoveGroupMemberPack.class);
            // 删除哪个成员id
            String member = pack.getMember();
            // 走到这步骤的时候，这个已经被删除了，所以这里查出所有的成员id没有，哪个删除的人
            List<String> members
                    = imGroupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());
            // 这里加一下
            members.add(member);
            // 然后全部通知一下
            for (String memberId : members) {
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)){
                    messageProducer.sendToUserExceptClient(memberId,command,data,clientInfo);
                }else{
                    messageProducer.sendToUser(memberId,command,data,clientInfo.getAppId());
                }
            }
        }
        // 修改成员信息的时候的tcp通知，通知所有管理员
        else if(command.equals(GroupEventCommand.UPDATED_MEMBER)){
            UpdateGroupMemberPack pack = o.toJavaObject(UpdateGroupMemberPack.class);
            // 被修改人的id
            String memberId = pack.getGroupId();
            // 获取到所有的管理员
            List<GroupMemberDto> groupManager
                    = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            // 将被修改人也要通知到，所以搞一个dto
            GroupMemberDto groupMemberDto = new GroupMemberDto();
            groupMemberDto.setMemberId(memberId);
            groupManager.add(groupMemberDto);
            // 全发一遍
            for (GroupMemberDto member : groupManager) {
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)){
                    messageProducer.sendToUserExceptClient(member.getMemberId(),command,data,clientInfo);
                }else{
                    messageProducer.sendToUser(member.getMemberId(),command,data,clientInfo.getAppId());
                }
            }
        }else{
            for (String memberId : groupMemberId) {
                // 如果clientType不为空，并且类型不是Web，那么一定就是app端发送的
                if(clientInfo.getClientType() != null
                        && clientInfo.getClientType() != ClientType.WEBAPI.getCode() && memberId.equals(userId)){
                    // 发送给除了本端的其他端
                    messageProducer.sendToUserExceptClient(memberId, command, data, clientInfo);
                }else{
                    // 全发
                    messageProducer.sendToUser(memberId, command, data, clientInfo.getAppId());
                }
            }
        }
    }
}
