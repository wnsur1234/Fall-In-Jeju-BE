package com.jeju.ormicamp.model.s3;

public class TourismMarkdownParser {

    private final String PREFIX = "https://jeju-ai-kb-310688446727.s3.ap-northeast-2.amazonaws.com/Back-map-data/restaurants";

    public  TouristPlace parse(String mdContent) {

        String name = null;
        String imageUrl = null;
        String roadAddress = null;
        Double lat = null;
        Double lon = null;
        PlaceType type = PlaceType.RESTAURANTS;

        String[] lines = mdContent.split("\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // 1. 이름 + 이미지
            if (line.startsWith("![") && line.contains("](")) {
                name = line.substring(
                        line.indexOf("[") + 1,
                        line.indexOf("]")
                );

                imageUrl = PREFIX + line.substring(
                        line.indexOf("(") + 2,
                        line.indexOf(")")
                );
            }

            // 2. 주소
            if (line.startsWith("- 주소:")) {
                roadAddress = line.replace("- 주소:", "").trim();
            }

            // 3. 위치
            if (line.startsWith("- 위치:")) {
                String location = line.replace("- 위치:", "").trim();

                // 🔒 방어: None / 빈 값
                if (location.isBlank() || location.equalsIgnoreCase("none")) {
                    continue;
                }

                String[] parts = location.split(",");
                if (parts.length != 2) {
                    continue;
                }

                try {
                    lat = Double.parseDouble(parts[0].trim());
                    lon = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    // 숫자 아님 → 스킵
                    continue;
                }
            }
        }

        // 🔥 핵심 조건: lat 또는 lon 없으면 저장 대상 아님
        if (lat == null || lon == null) {
            return null;
        }

        return new TouristPlace(
                name,
                lat,
                lon,
                roadAddress,
                imageUrl,
                type
        );
    }
}