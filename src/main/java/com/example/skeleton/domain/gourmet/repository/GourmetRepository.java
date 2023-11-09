package com.example.skeleton.domain.gourmet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.skeleton.domain.gourmet.entity.Gourmet;

public interface GourmetRepository extends JpaRepository<Gourmet, Long> {
        boolean existsByGourmetCode(String gourmetCode);

        Optional<Gourmet> findByGourmetCode(String gourmetCode);

        @Query("SELECT g FROM Gourmet g " +
                        "WHERE g.category = :category " +
                        "AND (6371 * " +
                        "acos(cos(radians(:clientLatitude)) * " +
                        "cos(radians(g.point.latitude)) * " +
                        "cos(radians(:clientLongitude) - radians(g.point.longitude)) + " +
                        "sin(radians(:clientLatitude)) * " +
                        "sin(radians(g.point.latitude))) <= 0.5) " +
                        "ORDER BY g.rating DESC " +
                        "LIMIT 5")
        List<Gourmet> findTop5GourmetsByCategoryAndPoint(
                        @Param("category") String category,
                        @Param("clientLatitude") double clientLatitude,
                        @Param("clientLongitude") double clientLongitude);

}