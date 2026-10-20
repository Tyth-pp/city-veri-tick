package com.city.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface ManagementMessageMapper {
    @Select("SELECT o.name organizerName,COALESCE(NULLIF(u.staff_name,''),u.username) submitterName FROM organizer o JOIN city_user u ON u.id=#{userId} WHERE o.id=#{organizerId}")
    Map<String, Object> submissionLabel(@Param("organizerId") Long organizerId, @Param("userId") Long userId);

    String SCOPE = " m.audience=#{role} AND (#{role}='PLATFORM_ADMIN' OR m.organizer_id=#{organizerId}) ";

    @Insert("INSERT INTO management_message(audience,organizer_id,event_type,title,content,target_path,created_at) VALUES(#{audience},#{organizerId},#{type},#{title},#{content},#{path},#{now})")
    int insert(@Param("audience") String audience, @Param("organizerId") Long organizerId, @Param("type") String type, @Param("title") String title, @Param("content") String content, @Param("path") String path, @Param("now") LocalDateTime now);

    @Select("SELECT m.id,m.event_type type,m.title,m.content,m.target_path targetPath,m.created_at createdAt,(r.message_id IS NOT NULL) isRead FROM management_message m LEFT JOIN management_message_read r ON r.message_id=m.id AND r.user_id=#{userId} WHERE " + SCOPE + " ORDER BY m.id DESC LIMIT 20 OFFSET #{offset}")
    List<Map<String, Object>> list(@Param("role") String role, @Param("organizerId") Long organizerId, @Param("userId") Long userId, @Param("offset") int offset);

    @Select("SELECT m.id,m.event_type type,m.title,m.content,m.target_path targetPath,m.created_at createdAt FROM management_message m WHERE " + SCOPE + " AND NOT EXISTS(SELECT 1 FROM management_message_read r WHERE r.message_id=m.id AND r.user_id=#{userId}) ORDER BY m.id DESC LIMIT 50")
    List<Map<String, Object>> unread(@Param("role") String role, @Param("organizerId") Long organizerId, @Param("userId") Long userId);

    @Select("SELECT m.id FROM management_message m WHERE " + SCOPE + " AND NOT EXISTS(SELECT 1 FROM management_message_read r WHERE r.message_id=m.id AND r.user_id=#{userId})")
    List<Long> unreadIds(@Param("role") String role, @Param("organizerId") Long organizerId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM management_message m WHERE " + SCOPE + " AND NOT EXISTS(SELECT 1 FROM management_message_read r WHERE r.message_id=m.id AND r.user_id=#{userId})")
    long unreadCount(@Param("role") String role, @Param("organizerId") Long organizerId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM management_message m WHERE " + SCOPE + " AND m.id=#{id}")
    long visible(@Param("role") String role, @Param("organizerId") Long organizerId, @Param("id") Long id);

    @Insert("INSERT IGNORE INTO management_message_read(message_id,user_id) VALUES(#{id},#{userId})")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
}
