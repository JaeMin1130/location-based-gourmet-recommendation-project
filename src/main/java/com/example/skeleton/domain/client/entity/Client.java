package com.example.skeleton.domain.client.entity;

import com.example.skeleton.global.model.Point;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", length = 20, nullable = false, unique = true)
    private String clientId;

    @Column(name = "password", nullable = false)
    private String password;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(length = 20)),
            @AttributeOverride(name = "longitude", column = @Column(length = 20))
    })
    private Point point;

    @Enumerated(value = EnumType.STRING)
    private Permission permission;

    // 회원 가입에 사용
    @Builder
    public Client(String clientId, String password) {
        this.clientId = clientId;
        this.password = password;
        this.point = Point.of(0.0, 0.0);
        this.permission = Permission.UNALLOWED;
    }

    public void setPoint(Double latitude, Double longitude) {
        this.point = Point.of(latitude, longitude);
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}