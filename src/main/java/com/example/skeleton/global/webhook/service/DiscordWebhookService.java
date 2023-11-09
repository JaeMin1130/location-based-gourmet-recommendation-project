package com.example.skeleton.global.webhook.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.skeleton.domain.client.entity.Client;
import com.example.skeleton.domain.client.entity.Permission;
import com.example.skeleton.domain.client.repository.ClientRepository;
import com.example.skeleton.domain.gourmet.entity.Gourmet;
import com.example.skeleton.domain.gourmet.repository.GourmetRepository;
import com.example.skeleton.global.model.Point;
import com.example.skeleton.global.webhook.dto.DiscordWebhookMessage;
import com.example.skeleton.global.webhook.dto.Embed;
import com.example.skeleton.global.webhook.dto.Embed.Field;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class DiscordWebhookService {
    private final WebClient client;
    private final ClientRepository clientRepo;
    private final GourmetRepository gourmetRepo;

    /*
     * webhook service 전체 로직
     * 
     * 1. 추천 허용한 클라이언트 리스트 조회
     * 2. 각 클라이언트 위치에 따라 500미터 이내의 식당 평점 높은 순으로 카테고리별 5개씩 조회
     * 3. request body 구성(Embed > Field)
     * 4. webhook로 request 전송(response 없음)
     * 
     */

    public void executeDiscordWebhook() {
        // 1. 추천 허용한 클라이언트 리스트 조회
        List<Client> clientList = clientRepo.findByPermission(Permission.ALLOWED);

        for (Client client : clientList) {

            Point clientPoint = client.getPoint();
            Double clientLatitude = clientPoint.getLatitude();
            Double clientLongitude = clientPoint.getLongitude();

            // 2. 각 클라이언트 위치에 따라 500미터 이내의 식당 평점 높은 순으로 카테고리별 5개씩 조회
            List<Gourmet> chinaGourmetList = gourmetRepo.findTop5GourmetsByCategoryAndPoint("중식",
                    clientLatitude, clientLongitude);
            List<Gourmet> japanGourmetList = gourmetRepo.findTop5GourmetsByCategoryAndPoint("일식",
                    clientLatitude, clientLongitude);
            List<Gourmet> fastfoodGourmetList = gourmetRepo.findTop5GourmetsByCategoryAndPoint("패스트 푸드",
                    clientLatitude, clientLongitude);

            // 3. request body 구성(Embed > Field)
            DiscordWebhookMessage message = configWebhookMessage(client.getClientId(), chinaGourmetList,
                    japanGourmetList, fastfoodGourmetList);

            // 4. webhook로 request 전송(response 없음)
            sendDiscordMessage(message);

        }
    }

    private DiscordWebhookMessage configWebhookMessage(String clientId, List<Gourmet> chinaGourmetList,
            List<Gourmet> japanGourmetList, List<Gourmet> fastfoodGourmetList) {

        List<Field> fieldList = new ArrayList<>();
        StringBuilder chinaFieldValue = configFieldValue(chinaGourmetList);
        StringBuilder japanFieldValue = configFieldValue(japanGourmetList);
        StringBuilder fastfoodFieldValue = configFieldValue(fastfoodGourmetList);

        fieldList.add(Field.builder().name("중식").value(chinaFieldValue.toString()).build());
        fieldList.add(Field.builder().name("일식").value(japanFieldValue.toString()).build());
        fieldList.add(Field.builder().name("패스트 푸드").value(fastfoodFieldValue.toString()).build());

        List<Embed> embedList = new ArrayList<>();
        embedList.add(Embed.builder().fields(fieldList).build());

        DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                .username("맛집 알림")
                .content(clientId + "님의 위치 반경 500m 이내 맛집 목록입니다.")
                .embeds(embedList)
                .build();

        return message;

    }

    private StringBuilder configFieldValue(List<Gourmet> gourmetList) {

        StringBuilder fieldValue = new StringBuilder("");
        for (int i = 0; i < gourmetList.size(); i++) {
            fieldValue.append(String.format("%s. %s\n", i, gourmetList.get(i)));
        }
        return fieldValue;

    }

    private void sendDiscordMessage(DiscordWebhookMessage message) {

        client.post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block();

    }
}
