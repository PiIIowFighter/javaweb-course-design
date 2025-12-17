package edu.bjfu.journal.mapper;

import edu.bjfu.journal.model.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser selectById(@Param("id") Long id);
    SysUser selectByUsername(@Param("username") String username);
    List<SysUser> list(@Param("keyword") String keyword, @Param("status") Integer status);

    int insert(SysUser u);
    int update(SysUser u);
    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updateLastLogin(@Param("id") Long id);
}
