package edu.bjfu.journal.mapper;

import edu.bjfu.journal.model.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    List<SysRole> listAll();
    List<SysRole> listByUserId(@Param("userId") Long userId);
    SysRole selectByCode(@Param("code") String code);
}
