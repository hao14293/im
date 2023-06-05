package com.hao14293.im.service.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hao14293.im.service.conversation.entity.ImConversationSetEntity;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;


/**
 * @author hao14293
 */
@Repository
public interface ImConversationSetMapper extends BaseMapper<ImConversationSetEntity> {

    // 更新已读坐标
    @Update(" update im_conversation_set set readed_sequence = #{readedSequence},sequence = #{sequence} " +
            " where conversation_id = #{conversationId} and app_id = #{appId} AND readed_sequence < #{readedSequence}")
    public void readMark(ImConversationSetEntity imConversationSetEntity);

    @Select(" select max(sequence) from im_conversation_set where app_id = #{appId} AND from_id = #{userId} ")
    Long getConversationSetMaxSeq(Integer appId, String userId);
}