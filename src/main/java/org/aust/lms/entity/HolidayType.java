package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "holiday_type")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class HolidayType {

    @Id
    private Long id;

    private String name;

    private Boolean subjectToMoon;

    private Instant lastModified;
}
