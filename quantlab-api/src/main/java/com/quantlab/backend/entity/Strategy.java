package com.quantlab.backend.entity;

import jakarta.persistence.*;

/**
 * Strategy entity - static reference table
 * e.g. code: "EMA_CROSS", name: "EMA Crossover"
 */
@Entity
@Table(name = "strategy")
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean supportsScreening = true;

    @Column(nullable = false)
    private Integer minLookbackDays = 20;

    public Strategy() {
    }

    public Strategy(Long id, String code, String name, String description, Boolean active,
                    Boolean supportsScreening, Integer minLookbackDays) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
        this.supportsScreening = supportsScreening;
        this.minLookbackDays = minLookbackDays;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getSupportsScreening() {
        return supportsScreening;
    }

    public void setSupportsScreening(Boolean supportsScreening) {
        this.supportsScreening = supportsScreening;
    }

    public Integer getMinLookbackDays() {
        return minLookbackDays;
    }

    public void setMinLookbackDays(Integer minLookbackDays) {
        this.minLookbackDays = minLookbackDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Strategy strategy = (Strategy) o;

        return id != null && id.equals(strategy.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
