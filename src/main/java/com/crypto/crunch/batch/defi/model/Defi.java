package com.crypto.crunch.batch.defi.model;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
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
    private String defiIconUrl;
    private String platformIconUrl;
    private String detailUrl;
    private DefiConf.DefiCoinType coinType;
    private List<DefiConf.DefiAttributeType> attributes;
    private List<DefiHistory> histories;
}
