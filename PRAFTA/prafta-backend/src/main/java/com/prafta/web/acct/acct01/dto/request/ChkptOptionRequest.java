package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 점검대상(CHKPT) 검색 옵션 요청 (ChkptSearchPop 용).
 * tb_chkpt_type_mgmt 를 사업장 + 점검구분(CHKLST_TYPE) 으로 필터.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChkptOptionRequest {
    private String siteCd;
    private String chklstType; // COM001
    private String chkptNm;    // 점검대상명 부분검색
}
