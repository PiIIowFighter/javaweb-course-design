package edu.bjfu.journal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRolePermMapper {
    List<String> listPermCodesByRoleIds(@Param("roleIds") List<Long> roleIds);
    int deleteByRoleId(@Param("roleId") Long roleId);
    int insert(@Param("roleId") Long roleId, @Param("permCode") String permCode);
}
