package edu.bjfu.journal.service;

import edu.bjfu.journal.mapper.SysLoginLogMapper;
import edu.bjfu.journal.mapper.SysOpLogMapper;
import edu.bjfu.journal.model.SysLoginLog;
import edu.bjfu.journal.model.SysOpLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    @Autowired private SysLoginLogMapper loginLogMapper;
    @Autowired private SysOpLogMapper opLogMapper;

    public List<SysLoginLog> loginLogs(String username) {
        return loginLogMapper.list(username);
    }

    public List<SysOpLog> opLogs(String username, String module) {
        return opLogMapper.list(username, module);
    }

    public void addOpLog(SysOpLog log) {
        opLogMapper.insert(log);
    }
}
