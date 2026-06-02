package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopMenu {
    private String id;
    private String label;
}