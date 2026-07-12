package com.prafta.web.baim.baim05.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.SetSlotTypeRequest;

/**
 * 슬롯 구분(SLOT_TYPE, SYS014) 변경 파라미터.
 *
 * <p>JWT 클레임 도출값(gvCmpnyCd/gvUserCd/gvAuthCd)만 신뢰한다(클라 바디 신뢰 금지).
 * 슬롯별로 변경할 구분 값이 다르므로 SlotItem 안에 slotType 을 포함한다.
 */
public record SetSlotTypeParam(
    List<SlotItem> slots
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    /** 슬롯 단건(사업장/슬롯번호) + 변경할 구분 값. */
    public record SlotItem(String siteCd, String slotNo, String slotType) {}

    public static SetSlotTypeParam from(SetSlotTypeRequest request, TokenInfo tokenInfo) {

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<SlotItem> slots = new ArrayList<>();
        for (SetSlotTypeRequest.Item item : request.getSlots()) {
            if (item == null || item.getSiteCd() == null || item.getSiteCd().isBlank()
                || item.getSlotNo() == null || item.getSlotNo().isBlank()
                || item.getSlotType() == null || item.getSlotType().isBlank()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // siteCd 길이 상한(varchar50), slotNo 숫자 1~4자리(varchar4·1~N) 형식 검증
            if (item.getSiteCd().length() > 50
                || !item.getSlotNo().matches("\\d{1,4}")) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            slots.add(new SlotItem(item.getSiteCd(), item.getSlotNo(), item.getSlotType()));
        }

        return new SetSlotTypeParam(
            slots
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
