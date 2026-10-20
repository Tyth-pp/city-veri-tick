package com.city.mapper;

import org.apache.ibatis.annotations.Insert;
import com.city.vo.OrganizerAccountVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.User;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Select;
import com.city.vo.PlatformUserViewVO;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM city_user WHERE id=#{userId} FOR UPDATE")
    User selectByIdForUpdate(@Param("userId") Long userId);

    @Select("SELECT o.id,o.name FROM organizer_member m JOIN organizer o ON o.id=m.organizer_id WHERE m.user_id=#{userId} ORDER BY o.id")
    List<OrganizerAccountVO> selectOrganizations(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT m.user_id) FROM organizer_member m JOIN sys_user_role ur ON ur.user_id=m.user_id JOIN sys_role r ON r.id=ur.role_id WHERE m.organizer_id=#{organizerId} AND r.code='ORGANIZER_ADMIN'")
    long countAdministrators(@Param("organizerId") Long organizerId);

    @Select("SELECT u.id, u.username, u.phone, u.nickname, u.staff_name, u.enabled, u.created_at, "
            + "COALESCE(GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ','), '') AS role_codes "
            + "FROM city_user u LEFT JOIN sys_user_role ur ON ur.user_id = u.id "
            + "LEFT JOIN sys_role r ON r.id = ur.role_id "
            + "GROUP BY u.id, u.username, u.phone, u.nickname, u.staff_name, u.enabled, u.created_at "
            + "ORDER BY u.created_at DESC")
    @Results({
            @Result(column = "id", property = "id"), @Result(column = "username", property = "username"),
            @Result(column = "phone", property = "phone"), @Result(column = "nickname", property = "nickname"),
            @Result(column = "staff_name", property = "name"),
            @Result(column = "enabled", property = "enabled"), @Result(column = "created_at", property = "createdAt"),
            @Result(column = "role_codes", property = "roleCodes")
    })
    List<PlatformUserViewVO> selectPlatformUsers();

    @Select("SELECT COUNT(1) FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.code = #{roleCode}")
    long countRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Select("SELECT u.*, r.code AS role "
            + "FROM city_user u JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id WHERE u.username = #{username} "
            + "ORDER BY FIELD(r.code, 'PLATFORM_ADMIN', 'ORGANIZER_ADMIN', 'VERIFIER', 'USER') LIMIT 1")
    @Results(@Result(column = "role", property = "role"))
    User selectByUsernameWithRole(String username);

    @Select("SELECT u.*, r.code AS role FROM city_user u JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id WHERE u.phone = #{phone} "
            + "ORDER BY FIELD(r.code, 'PLATFORM_ADMIN', 'ORGANIZER_ADMIN', 'VERIFIER', 'USER') LIMIT 1")
    @Results(@Result(column = "role", property = "role"))
    User selectByPhoneWithRole(String phone);

    @Select("SELECT * FROM city_user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(@Param("phone") String phone);

    @Select("SELECT id FROM sys_role WHERE code = #{roleCode} LIMIT 1")
    Long selectRoleId(@Param("roleCode") String roleCode);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
