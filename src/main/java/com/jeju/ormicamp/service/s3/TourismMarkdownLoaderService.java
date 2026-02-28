package com.jeju.ormicamp.service.s3;

import com.jeju.ormicamp.infrastructure.repository.s3.RestaurantImageLoader;
import com.jeju.ormicamp.infrastructure.repository.s3.TouristPlaceJdbcRepository;
import com.jeju.ormicamp.model.s3.TourismMarkdownParser;
import com.jeju.ormicamp.model.s3.TouristPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourismMarkdownLoaderService {

    private static final String BUCKET = "jeju-ai-kb-310688446727";
    private static final String PREFIX = "Back-map-data/restaurants/";
    private static final int MAX_COUNT = 100;

    private final TouristPlaceJdbcRepository repository;

    private final TourismMarkdownParser parser = new TourismMarkdownParser();

    private final S3Client s3Client;

    public void migrate() {
        List<String> urls = RestaurantImageLoader.loadSortedImageUrls(s3Client);
        repository.updateImages(urls);
    }

    public void loadAll() {

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(BUCKET)
                .prefix(PREFIX)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        int processed = 0;

        for (S3Object object : response.contents()) {

            if (!object.key().endsWith(".md")) {
                continue;
            }

            String md = readMarkdown(object.key());
            TouristPlace place = parser.parse(md);

            // 🔥 lat/lon 없는 데이터 스킵
            if (place == null) {
                continue;
            }

            repository.save(place);
            processed++;

            if (processed >= MAX_COUNT) {
                break;
            }
        }
    }

    private String readMarkdown(String key) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(request)
                .asUtf8String();
    }
}