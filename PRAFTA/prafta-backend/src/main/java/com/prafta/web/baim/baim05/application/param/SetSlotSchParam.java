package com.prafta.web.baim.baim05.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.SetSlotSchRequest;

/**
 * 슬롯 기본 근무타입(DEFAULT_SCH_CD) 지정/해제 파라미터.
 *
 * <p>JWT 클레임 도출값(gvCmpnyCd/gvUserCd/gvAuthCd)만 신뢰한다(클라 바디 신뢰 금지).
 * 슬롯별로 지정할 근무타입이 다를 수 있으므로 SlotItem 안에 schCd 를 포함한다(빈값=해제).
 * SetSlotNodeParam 패턴 미러.
 */
public record SetSlotSchParam(
    List<SlotItem> slots
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    /** 슬롯 단건(사업장/슬롯번호) + 지정할 기본 근무타입(빈값=NULL 해제). */
    public record SlotItem(String siteCd, String slotNo, String schCd) {}

    public static SetSlotSchParam from(SetSlotSchRequest request, TokenInfo tokenInfo) {

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<SlotItem> slots = new ArrayList<>();
        for (SetSlotSchRequest.Item item : request.getSlots()) {
            // siteCd/slotNo 는 필수, schCd 는 선택(빈값=해제)
            if (item == null || item.getSiteCd() == null || item.getSiteCd().isBlank()
                || item.getSlotNo() == null || item.getSlotNo().isBlank()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // siteCd 길이 상한(varchar50), slotNo 숫자 1~4자리, schCd 길이 상한(varchar20) 형식 검증
            if (item.getSiteCd().length() > 50
                || !item.getSlotNo().matches("\\d{1,4}")
                || (item.getSchCd() != null && item.getSchCd().length() > 20)) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // schCd 빈문자/blank 는 NULL(해제)로 정규화
            String schCd = (item.getSchCd() == null || item.getSchCd().isBlank())
                ? null : item.getSchCd();
            slots.add(new SlotItem(item.getSiteCd(), item.getSlotNo(), schCd));
        }

        return new SetSlotSchParam(
            slots
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
