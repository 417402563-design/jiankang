package cn.edu.sdua.jiankang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色实体 —— 用户权限管理模块
 */
@Entity
@Table(name = "sys_role")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色标识,如 ROLE_ADMIN / ROLE_EDITOR / ROLE_USER */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 角色描述 */
    @Column(length = 200)
    private String description;
}
