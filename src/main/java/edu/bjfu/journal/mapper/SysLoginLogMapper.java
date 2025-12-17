package edu.bjfu.journal.mapper;

import edu.bjfu.journal.model.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysLoginLogMapper {
    int insert(SysLoginLog log);
    List<SysLoginLog> list(@Param("username") String username);
}
