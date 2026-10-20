package com.city.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    @Select("SELECT EXISTS(SELECT 1 FROM activity WHERE id = #{id} AND status = 'PUBLISHED')")
    boolean isPublished(@Param("id") Long id);
}
