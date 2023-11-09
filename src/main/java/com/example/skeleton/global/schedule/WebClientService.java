package com.example.skeleton.global.schedule;

import com.example.skeleton.domain.gourmet.entity.Gourmet;
import com.example.skeleton.domain.gourmet.model.DistrictInfo;
import com.example.skeleton.domain.gourmet.model.Employee;
import com.example.skeleton.global.model.Address;
import com.example.skeleton.global.model.Point;
import com.example.skeleton.global.schedule.model.OpenApiType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebClientService {

    @Value("${open-api.secret-key}")
    private String openApiSecretKey;

    @Value("${open-api.url}")
    private String url;

    private WebClient webClient;

    private static final int DEFAULT_PAGE_INDEX = 1;
    private static final int PAGE_SIZE = 1000;

    private final ExchangeStrategies exchangeStrategies;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(url)
                .build();

        log.info("WebClient init completed");
    }

    public List<Gourmet> post(String path, int pageIndex) {

        String block = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/" + path)
                        .queryParam("type", OpenApiType.JSON.toLowerCase())
                        .queryParam("pIndex", pageIndex)
                        .queryParam("pSize", PAGE_SIZE)
                        .queryParam("KEY", openApiSecretKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<Gourmet> result = new ArrayList<>();

        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(block);
            JSONArray jsonResponse = (JSONArray) jsonObject.get(path);
            JSONObject jsonRow = (JSONObject) jsonResponse.get(1);
            JSONArray jsonRowArray = (JSONArray) jsonRow.get("row");

            result = jsonRowArray.stream().map(row -> makeGourmetEntity((JSONObject) row))
                    .toList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public long getOpenApiTotalPage(String path) {
        long result = 0;

        String block = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/" + path)
                        .queryParam("type", OpenApiType.JSON.toLowerCase())
                        .queryParam("pIndex", DEFAULT_PAGE_INDEX)
                        .queryParam("pSize", PAGE_SIZE)
                        .queryParam("KEY", openApiSecretKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(block);
            JSONArray jsonResponse = (JSONArray) jsonObject.get(path);

            result = getTotalPage(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private long getTotalPage(JSONArray jsonResponse) {
        JSONObject jsonHead = (JSONObject) jsonResponse.get(0);
        JSONArray jsonHeadArray = (JSONArray) jsonHead.get("head");
        JSONObject totalCount = (JSONObject) jsonHeadArray.get(0);
        Long total = (Long) totalCount.get("list_total_count");

        if (total == null || total == 0)
            return 0;

        return total % PAGE_SIZE == 0 ? (total / PAGE_SIZE) : (total / PAGE_SIZE) + 1;
    }

    public Gourmet makeGourmetEntity(JSONObject item) {
        return Gourmet.builder()
                .name((String) item.get("BIZPLC_NM"))
                .category((String) item.get("SANITTN_BIZCOND_NM"))
                .point(Point.of((Double) item.get("REFINE_WGS84_LAT"), (Double) item.get("REFINE_WGS84_LOGT")))
                .address(Address.of((String) item.get("REFINE_ROADNM_ADDR"),
                        (String) item.get("REFINE_LOTNO_ADDR"),
                        (String) item.get("REFINE_ZIP_CD")))
                .employee(Employee.convertToEmployee(item))
                .districtInfo(DistrictInfo.convertToDistrictInfo(item))
                .isOpen((String) item.get("BSN_STATE_NM"))
                .build();
    }

}