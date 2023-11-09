package com.example.skeleton.domain.district.service;

import com.example.skeleton.IntegrationTest;
import com.example.skeleton.domain.district.entity.District;
import com.example.skeleton.global.model.Point;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

class DistrictServiceTest extends IntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void cachingSggInRedis() {
        // given
        District testDistrict1 = District.of(1L, "강원", "강릉시", Point.of(128.8784972,	37.74913611));
        HashOperations<String, String, Double> hashDistricts = redisTemplate.opsForHash();
        String key = "District::" + testDistrict1.getDosi() + "::" + testDistrict1.getSgg();

        // when
        hashDistricts.putAll(key, testDistrict1.getPoint().toMap());

        // then
        Map<String, Double> locationMap = hashDistricts.entries(key);
        Assertions.assertThat(locationMap.keySet()).containsOnly("latitude", "longitude");
        Assertions.assertThat(locationMap.values()).containsOnly(testDistrict1.getPoint().getLatitude(), testDistrict1.getPoint().getLongitude());

        Long size = hashDistricts.size(key);
        Assertions.assertThat(size).isEqualTo(locationMap.size());

        redisTemplate.delete(key);
    }
}