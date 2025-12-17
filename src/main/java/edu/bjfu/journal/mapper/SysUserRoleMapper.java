package edu.bjfu.journal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserRoleMapper {
    int deleteByUserId(@Param("userId") Long userId);
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
