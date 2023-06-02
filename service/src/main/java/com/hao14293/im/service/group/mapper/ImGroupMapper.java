package com.hao14293.im.service.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupEntity> {
    @Select(" <script> " +
            " select max(sequence) from im_group where app_id = #{appId} and group_id in " +
            "<foreach collection='groupId' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " </script> ")
    Long getGroupMaxSeq(Collection<String> groupId, Integer appId);
}
