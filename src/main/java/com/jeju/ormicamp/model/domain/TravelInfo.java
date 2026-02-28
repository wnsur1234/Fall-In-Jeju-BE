package com.jeju.ormicamp.model.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.jeju.ormicamp.model.code.Region;
import com.jeju.ormicamp.model.code.Theme;
import com.jeju.ormicamp.model.code.Language;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TravelInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long Capacity;

    private Long Money;

    @Enumerated(EnumType.STRING)
    private Region region;  // 여행 지역

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "travel_info_themes", joinColumns = @JoinColumn(name = "travel_info_id"))
    private List<Theme> themes;  // 여행 테마 (복수 선택 가능)

    @Enumerated(EnumType.STRING)
    private Language language;  // 언어 선택

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime updateDate;


    /**
     * 수정한 날짜를 저장
     * 단, 수정하지 않은 부분은 null 값으로 오기 때문에 저장 x
     * @param startDate 시작일
     * @param endDate 종료일
     */
    public void updateDate(LocalDate startDate, LocalDate endDate) {

        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }

    }


}
