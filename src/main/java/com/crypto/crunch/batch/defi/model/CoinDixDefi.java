package com.crypto.crunch.batch.defi.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CoinDixDefi {
    private String id;
    private String name;
    private String icon;
    private String chain;
    private String protocol;
    private Double base;
    private Double reward;
    private Double apy;
    private Long tvl;
    private Integer risk;
    private String link;
}
