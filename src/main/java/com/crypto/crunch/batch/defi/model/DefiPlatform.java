package com.crypto.crunch.batch.defi.model;

import com.crypto.crunch.batch.defi.conf.DefiConf;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DefiPlatform {
    private String id;
    private String name;
    private List<DefiConf.DefiAttributeType> attributes;
}
