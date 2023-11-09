package com.example.skeleton.domain.district.service;

import com.example.skeleton.domain.district.entity.District;
import com.example.skeleton.domain.district.repository.DistrictRepository;
import com.example.skeleton.global.model.Point;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository districtRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    /**
     * caching을 이용하지 않고 DB에서 시군구 조회
     * */
    public List<District> getAllDistrictsWithoutRedis() {
        return districtRepository.findAll();
    }

    /**
     * caching을 통한 시군구 조회
     * */
    // Cacheable의 value, key 속성을 합쳐서 redis의 키가 생성됩니다. ex) value::key
    // value는 CacheConfig에서 설정한 keyName
    // unless : 캐싱 조건 설정, Spring Expression Language (SpEL) expression으로 작성해야합니다.
    @Cacheable(value = "district", key = "'allDistricts'", unless = "#result == null")
    public List<District> getAllDistricts() {
        return districtRepository.findAll();
    }

    /**
     * 시군구 위도, 경도 데이터를 담은 csv 업로드
     * @param file - MultipartFile : csv file
     * */
    @Transactional
    public void uploadSggCsv(MultipartFile file) throws IOException {

        List<District> districtList = new ArrayList<>();

        // csv 파일 데이터 전처리
        try {
            // 파일의 내용을 바이트 배열로 가져온 후, 이를 문자열로 변환합니다. 그 뒤 줄바꿈으로 나누어 각 행을 얻습니다.
            // 줄바꿈 문자 형식 : Windows -> \\r\\n, Linux -> \\n, MacOS -> \\r
            // 모든 운영체제의 줄바꿈 문자를 표함하는 정규식 : \\r?\\n|\\r
            String[] rows = new String(file.getBytes()).split("\\r?\\n|\\r");

            for(int i = 1; i < rows.length; i++) {
                // csv 파일이었기 때문에 쉼표로 나누어 각 열을 얻습니다.
                String[] columns = rows[i].split(",");

                // District 객체를 생성해 리스트에 추가합니다.
                districtList.add(
                        District.of((long) i, columns[0], columns[1], Point.of(Double.parseDouble(columns[3]), Double.parseDouble(columns[2])
                )));
            }
        } catch (IOException e) {
            log.error("IOException during reading file", e);
        }

        // 데이터 저장 전 삭제
        districtRepository.deleteAll();

        // 데이터 DB에 저장
        districtRepository.saveAll(districtList);

/*        // Redis에 각각의 시군구 정보 캐싱 처리
        cachingSggInRedis(districtList);*/
    }

    // redisTemplate를 이용한 캐싱 입니다. 처음에 사용했다가 @Cacheable을 사용하기 위해 주석처리했습니다.
/*    private void cachingSggInRedis(List<District> districtList) {

        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();

        for(District district : districtList) {
            // key : District::{dosi}::{sgg}
            String key = "District::" + district.getDosi() + "::" + district.getSgg();
            hashOperations.putAll(key, district.getPoint().toMap());
        }
    }*/
}
