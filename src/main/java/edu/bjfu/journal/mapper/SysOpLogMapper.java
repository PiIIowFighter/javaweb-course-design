package edu.bjfu.journal.mapper;

import edu.bjfu.journal.model.SysOpLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOpLogMapper {
    int insert(SysOpLog log);
    List<SysOpLog> list(@Param("username") String username, @Param("module") String module);
}
