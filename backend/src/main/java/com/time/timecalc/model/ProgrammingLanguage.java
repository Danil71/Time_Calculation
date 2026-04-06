package com.time.timecalc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "programming_languages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammingLanguage {
    @Id
    @Column(length = 50)
    private String name;

    @Column(name = "loc_per_fp", nullable = false)
    private Integer locPerFp;
}
