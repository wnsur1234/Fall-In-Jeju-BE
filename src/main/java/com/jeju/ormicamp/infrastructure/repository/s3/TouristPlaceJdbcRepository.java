package com.jeju.ormicamp.infrastructure.repository.s3;

import com.jeju.ormicamp.model.s3.TouristPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TouristPlaceJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public void save(TouristPlace place) {

        String sql = """
            INSERT INTO tourist_place
            (name, lat, lon, road_address, image_url, type, score)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                place.getName(),
                place.getLat(),
                place.getLon(),
                place.getRoadAddress(),
                place.getImageUrl(),
                place.getType().toString(),
                0
        );
    }

    public void updateImages(List<String> imageUrls) {

        String sql = """
                    UPDATE tourist_place
                    SET image_url = ?
                    WHERE id = ?
                """;

        int startId = 301;

        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update(
                    sql,
                    imageUrls.get(i),
                    startId + i
            );
        }
    }

}
