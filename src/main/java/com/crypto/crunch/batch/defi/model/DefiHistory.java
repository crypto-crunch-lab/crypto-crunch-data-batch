package com.crypto.crunch.batch.defi.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DefiHistory {
    private Double apy;
    private Double base;
    private Double reward;
    private Long tvl;
    private String date;
}
