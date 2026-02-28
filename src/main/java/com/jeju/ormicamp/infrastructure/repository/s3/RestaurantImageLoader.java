package com.jeju.ormicamp.infrastructure.repository.s3;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.stream.Collectors;

public class RestaurantImageLoader {
    private static final String BUCKET = "jeju-ai-kb-310688446727";
    private static final String PREFIX = "Back-map-data/restaurants/images/";

    public static List<String> loadSortedImageUrls(S3Client s3Client) {

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(BUCKET)
                .prefix(PREFIX)
                .build();

        return s3Client.listObjectsV2(request)
                .contents()
                .stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith(".jpg"))
                .sorted() // 🔥 핵심
                .map(key ->
                        "https://jeju-ai-kb-310688446727.s3.ap-northeast-2.amazonaws.com/" + key
                )
                .collect(Collectors.toList());
    }
}
