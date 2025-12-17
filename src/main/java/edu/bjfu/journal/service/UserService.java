package edu.bjfu.journal.service;

import edu.bjfu.journal.mapper.SysRoleMapper;
import edu.bjfu.journal.mapper.SysUserMapper;
import edu.bjfu.journal.mapper.SysUserRoleMapper;
import edu.bjfu.journal.model.SysRole;
import edu.bjfu.journal.model.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysUserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public List<SysUser> list(String keyword, Integer status) {
        return userMapper.list(keyword, status);
    }

    public SysUser get(Long id) {
        return userMapper.selectById(id);
    }

    public void createUser(String username, String rawPassword, String realName, String email, String phone, Integer status, String roleCode) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRealName(realName);
        u.setEmail(email);
        u.setPhone(phone);
        u.setStatus(status == null ? 1 : status);
        userMapper.insert(u);

        if (roleCode != null && !roleCode.isEmpty()) {
            SysRole role = roleMapper.selectByCode(roleCode);
            if (role != null) {
                userRoleMapper.insert(u.getId(), role.getId());
            }
        }
    }

    public void updateUser(SysUser u) {
        userMapper.update(u);
    }

    public void deleteUser(Long id) {
        // 先删映射
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    public void approve(Long id) {
        userMapper.updateStatus(id, 1);
    }

    public void disable(Long id) {
        userMapper.updateStatus(id, 2);
    }
}
