package edu.bjfu.journal.service;

import edu.bjfu.journal.mapper.SysLoginLogMapper;
import edu.bjfu.journal.mapper.SysRoleMapper;
import edu.bjfu.journal.mapper.SysRolePermMapper;
import edu.bjfu.journal.mapper.SysUserMapper;
import edu.bjfu.journal.model.SysLoginLog;
import edu.bjfu.journal.model.SysRole;
import edu.bjfu.journal.model.SysUser;
import edu.bjfu.journal.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysRolePermMapper rolePermMapper;
    @Autowired private SysLoginLogMapper loginLogMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SysUser login(String username, String password, HttpServletRequest request) {
        SysUser u = userMapper.selectByUsername(username);
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(IpUtil.getClientIp(request));
        log.setUa(request.getHeader("User-Agent"));

        if (u == null) {
            log.setSuccess(0);
            log.setMsg("用户不存在");
            loginLogMapper.insert(log);
            return null;
        }
        log.setUserId(u.getId());

        if (u.getStatus() != null && u.getStatus() != 1) {
            log.setSuccess(0);
            log.setMsg("账号未启用（status=" + u.getStatus() + ")");
            loginLogMapper.insert(log);
            return null;
        }

        boolean ok = encoder.matches(password, u.getPasswordHash());
        if (!ok) {
            log.setSuccess(0);
            log.setMsg("密码错误");
            loginLogMapper.insert(log);
            return null;
        }

        userMapper.updateLastLogin(u.getId());
        log.setSuccess(1);
        log.setMsg("登录成功");
        loginLogMapper.insert(log);
        return u;
    }

    public List<SysRole> rolesOf(Long userId) {
        return roleMapper.listByUserId(userId);
    }

    public Set<String> permsOfRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(rolePermMapper.listPermCodesByRoleIds(roleIds));
    }

    public Set<String> roleCodesOf(Long userId) {
        return rolesOf(userId).stream().map(SysRole::getCode).collect(Collectors.toSet());
    }
}
