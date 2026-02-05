package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class StrategyResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("supportsScreening")
    private Boolean supportsScreening;

    @JsonProperty("minLookbackDays")
    private Integer minLookbackDays;

    public StrategyResponse() {
    }

    public StrategyResponse(Long id, String code, String name, String description,
                            Boolean supportsScreening, Integer minLookbackDays) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.supportsScreening = supportsScreening;
        this.minLookbackDays = minLookbackDays;
    }

    /**
     * Legacy constructor for backward compatibility.
     * @deprecated Use the full constructor with all metadata fields
     */
    @Deprecated
    public StrategyResponse(Long id, String code, String name) {
        this(id, code, name, null, true, 20);
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
        StrategyResponse that = (StrategyResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(code, that.code) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(supportsScreening, that.supportsScreening) &&
               Objects.equals(minLookbackDays, that.minLookbackDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, description, supportsScreening, minLookbackDays);
    }

    @Override
    public String toString() {
        return "StrategyResponse{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", supportsScreening=" + supportsScreening +
                ", minLookbackDays=" + minLookbackDays +
                '}';
    }
}
