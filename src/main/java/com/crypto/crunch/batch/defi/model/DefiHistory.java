package com.crypto.crunch.batch.defi.model;

import com.crypto.crunch.batch.defi.conf.DefiConf;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DefiHistory<T> {
    private DefiConf.DefiHistoryType historyType;
    private T value;
    private String syncYmd;
}
