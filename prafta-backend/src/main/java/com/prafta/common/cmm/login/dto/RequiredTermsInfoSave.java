package com.prafta.common.cmm.login.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequiredTermsInfoSave{
    String userId;
    String termsId;
    String termsVersion;
    String agrYn;
}
