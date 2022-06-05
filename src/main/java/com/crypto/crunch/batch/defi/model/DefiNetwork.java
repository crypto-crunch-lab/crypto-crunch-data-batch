package com.crypto.crunch.batch.defi.model;

import lombok.*;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DefiNetwork {
    private String name;
    private String networkIconUrl;
}
