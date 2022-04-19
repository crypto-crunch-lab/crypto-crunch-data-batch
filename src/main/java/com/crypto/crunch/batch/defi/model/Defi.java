package com.crypto.crunch.batch.defi.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class Defi {
    private String id;
    private String name;
    private String platform;
    private String network;
    private Double base;
    private Double reward;
    private Double apy;
    private Long tvl;
    private Integer risk;
    private String icon;
    private String link;
}
