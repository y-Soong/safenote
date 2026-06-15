package com.prafta.web.baim.baim05.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.SetSlotFixedRequest;
import com.prafta.web.baim.baim05.dto.request.SlotItemRequest;

public record SetSlotFixedParam(
    List<SlotItem> slots
    , String fixedYn
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    /** 슬롯 단건(사업장/슬롯번호). */
    public record SlotItem(String siteCd, String slotNo) {}

    public static SetSlotFixedParam from(SetSlotFixedRequest request, TokenInfo tokenInfo) {

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 고정여부는 'Y'(점유 유지) 또는 'N'(점유 해지)만 허용
        String fixedYn = request.getFixedYn();
        if (!"Y".equals(fixedYn) && !"N".equals(fixedYn))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<SlotItem> slots = new ArrayList<>();
        for (SlotItemRequest item : request.getSlots()) {
            if (item == null || item.getSiteCd() == null || item.getSiteCd().isBlank()
                || item.getSlotNo() == null || item.getSlotNo().isBlank()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // siteCd 길이 상한(varchar50), slotNo 숫자 1~4자리(varchar4·1~N) 형식 검증
            if (item.getSiteCd().length() > 50
                || !item.getSlotNo().matches("\\d{1,4}")) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            slots.add(new SlotItem(item.getSiteCd(), item.getSlotNo()));
        }

        return new SetSlotFixedParam(
            slots
            , fixedYn
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
