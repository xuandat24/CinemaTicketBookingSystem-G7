package com.G7.CTBS.service;

import com.G7.CTBS.entity.Role;
import com.G7.CTBS.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Integer id) {
        // Chuyển Integer thành Long và dùng hàm findById có sẵn
        return roleRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }
}