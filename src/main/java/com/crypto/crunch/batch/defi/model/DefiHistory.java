package com.crypto.crunch.batch.defi.model;

import com.crypto.crunch.batch.defi.conf.DefiConf;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class DefiHistory<T> {
    private DefiConf.DefiHistoryType historyType;
    private T value;
    private String syncYmd;
}
