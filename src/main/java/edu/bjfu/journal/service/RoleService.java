package edu.bjfu.journal.service;

import edu.bjfu.journal.mapper.SysRoleMapper;
import edu.bjfu.journal.mapper.SysRolePermMapper;
import edu.bjfu.journal.model.SysRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysRolePermMapper rolePermMapper;

    public List<SysRole> listAll() { return roleMapper.listAll(); }

    public List<String> permsByRoleIds(List<Long> roleIds) {
        return rolePermMapper.listPermCodesByRoleIds(roleIds);
    }

    public void replacePerms(Long roleId, List<String> permCodes) {
        rolePermMapper.deleteByRoleId(roleId);
        if (permCodes == null) return;
        for (String p : permCodes) {
            if (p != null && !p.trim().isEmpty()) {
                rolePermMapper.insert(roleId, p.trim());
            }
        }
    }
}
