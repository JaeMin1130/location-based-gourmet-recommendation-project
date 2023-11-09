package com.example.skeleton.domain.gourmet.service;

import com.example.skeleton.domain.client.entity.Client;
import com.example.skeleton.domain.client.repository.ClientRepository;
import com.example.skeleton.domain.gourmet.entity.Gourmet;
import com.example.skeleton.domain.gourmet.repository.GourmetRepository;
import com.example.skeleton.domain.rating.entity.Rating;
import com.example.skeleton.domain.rating.repository.RatingRepository;
import com.example.skeleton.global.model.Address;
import com.example.skeleton.global.model.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GourmetInitService implements CommandLineRunner {
    private final GourmetRepository gourmetRepository;
    private final ClientRepository clientRepository;
    private final RatingRepository ratingRepository;

    @Override
    public void run(String... args) throws Exception {

        // == 음식점 == //
        Gourmet gourmet1 = Gourmet.builder()
                .name("음식점 1")
                .category("카테고리1")
                .point(Point.of(0.0, 0.0))
                .address(Address.of("도로명1", "16", "16639"))
                .isOpen("open").build();

        Gourmet gourmet2 = Gourmet.builder()
                .name("음식점 2")
                .category("카테고리2")
                .point(Point.of(0.0, 0.0))
                .address(Address.of("도로명2", "16", "16639"))
                .isOpen("open").build();

        // == 사용자 == //
        Client client1 = Client.builder()
                .clientId("abc1")
                .password("abc1**")
                .build();

        Client client2 = Client.builder()
                .clientId("abc2")
                .password("abc2**")
                .build();

        // == 평점 == //
        Rating rating1 = Rating.builder()
                .client(client1)
                .gourmet(gourmet1)
                .score(5)
                .content("정말 맛있어요")
                .build();

        Rating rating2 = Rating.builder()
                .client(client2)
                .gourmet(gourmet1)
                .score(0)
                .content("노맛.. 가지마세요.")
                .build();

        gourmetRepository.saveAll(List.of(gourmet1, gourmet2));
        clientRepository.saveAll(List.of(client1, client2));
        ratingRepository.saveAll(List.of(rating1, rating2));
    }
}