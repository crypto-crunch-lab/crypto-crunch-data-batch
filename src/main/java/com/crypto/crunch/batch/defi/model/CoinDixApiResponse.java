package com.crypto.crunch.batch.defi.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CoinDixApiResponse {
    private Boolean hasPrevPage;
    private Boolean hasNextPage;
    private Integer total;
    private Integer totalPages;
    private List<CoinDixDefi> data;
}
