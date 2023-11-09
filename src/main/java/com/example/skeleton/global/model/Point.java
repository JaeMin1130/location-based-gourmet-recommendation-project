package com.example.skeleton.global.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.Map;

@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Embeddable
public class Point {

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public static Point of(Double latitude, Double longitude) {
        return new Point(latitude, longitude);
    }

    public Map<String, Double> toMap() {
        return Map.of("latitude", this.latitude, "longitude", this.longitude);
    }

}