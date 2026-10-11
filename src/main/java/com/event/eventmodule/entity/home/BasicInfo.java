package com.event.eventmodule.entity.home;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "basic_info",schema = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicInfo {

    @Id
    private Long id; // always = 1

    private Long totalEventsHosted;
    private Long totalStudentParticipated;
    private Long totalCollegeConnected;

}

