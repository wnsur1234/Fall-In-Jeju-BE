package com.jeju.ormicamp.model.domain;

import com.jeju.ormicamp.model.dto.place.PlaceResDto;
import com.jeju.ormicamp.model.s3.PlaceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tourist_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "road_address", nullable = false)
    private String roadAddress;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PlaceType type;


}
