package com.project.sneakermarket.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;


    /*
     * 不再使用 unique = true
     *
     * 因為我們要允許：
     *
     * Malice deleted_at != NULL
     * Malice deleted_at = NULL
     *
     * 同時存在。
     *
     * 唯一性改由 SQL Server Filtered Unique Index 控制。
     */
    @Column(
        name = "account",
        nullable = false,
        length = 50
    )
    private String account;


    @Column(
        name = "password_hash",
        nullable = false,
        length = 255
    )
    private String passwordHash;


    @Column(
        name = "name",
        nullable = false,
        length = 50
    )
    private String name;


    /*
     * Email 同樣取消 unique = true
     *
     * 避免已刪除會員的 Email
     * 阻止重新註冊。
     */
    @Column(
        name = "email",
        nullable = false,
        length = 100
    )
    private String email;


    @Column(
        name = "phone",
        nullable = false,
        length = 20
    )
    private String phone;


    @Column(
        name = "address",
        nullable = true,
        length = 255
    )
    private String address;


    @Column(
        name = "role",
        nullable = false,
        length = 20
    )
    private String role;


    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    /*
     * NULL
     * = 正常會員
     *
     * NOT NULL
     * = 已刪除會員
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @PrePersist
    public void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        /*
         * 新建立會員一定是有效會員
         */
        deletedAt = null;

        if (role == null) {
            role = "ROLE_MEMBER";
        }
    }
    @PreUpdate
    public void preUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}