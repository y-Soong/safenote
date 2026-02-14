package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Buttons {
    private String search;
    private String create;
    private String save;
    private String delete;
    private String excel;
}