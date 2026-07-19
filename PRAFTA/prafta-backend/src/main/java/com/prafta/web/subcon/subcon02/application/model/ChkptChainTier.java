package com.prafta.web.subcon.subcon02.application.model;

import java.util.Map;

/**
 * 점검 결과 write-through 체인의 한 티어(qa M-3).
 *
 * <p>좌표(회사/사업장/점검대상) + 표시용 인접 1차 회사(viaCmpnyCd, plan D4) + 기점 문항코드 → 그 티어 문항코드 매핑.
 * 매핑에 없는 문항은 그 티어에 대응 문항이 없다는 뜻이므로 전파 대상에서 제외한다.
 */
public record ChkptChainTier(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String viaCmpnyCd
    , Map<String, String> itemCdByOriginItemCd
){

    /** 기점 문항코드에 대응하는 이 티어의 문항코드(없으면 null). */
    public String resolveItemCd(String originItemCd) {
        return itemCdByOriginItemCd.get(originItemCd);
    }
}
