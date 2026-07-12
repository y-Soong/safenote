package com.prafta.web.baim.baim05.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.SetSlotNodeRequest;

/**
 * 슬롯 소속부서(NODE_CD) 지정/해제 파라미터.
 *
 * <p>JWT 클레임 도출값(gvCmpnyCd/gvUserCd/gvAuthCd)만 신뢰한다(클라 바디 신뢰 금지).
 * 슬롯별로 지정할 부서가 다르므로 SlotItem 안에 nodeCd 를 포함한다(빈값=해제).
 */
public record SetSlotNodeParam(
    List<SlotItem> slots
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    /** 슬롯 단건(사업장/슬롯번호) + 지정할 소속부서(빈값=NULL 해제). */
    public record SlotItem(String siteCd, String slotNo, String nodeCd) {}

    public static SetSlotNodeParam from(SetSlotNodeRequest request, TokenInfo tokenInfo) {

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<SlotItem> slots = new ArrayList<>();
        for (SetSlotNodeRequest.Item item : request.getSlots()) {
            // siteCd/slotNo 는 필수, nodeCd 는 선택(빈값=해제)
            if (item == null || item.getSiteCd() == null || item.getSiteCd().isBlank()
                || item.getSlotNo() == null || item.getSlotNo().isBlank()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // siteCd 길이 상한(varchar50), slotNo 숫자 1~4자리, nodeCd 길이 상한(varchar50) 형식 검증
            if (item.getSiteCd().length() > 50
                || !item.getSlotNo().matches("\\d{1,4}")
                || (item.getNodeCd() != null && item.getNodeCd().length() > 50)) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // nodeCd 빈문자/blank 는 NULL(해제)로 정규화
            String nodeCd = (item.getNodeCd() == null || item.getNodeCd().isBlank())
                ? null : item.getNodeCd();
            slots.add(new SlotItem(item.getSiteCd(), item.getSlotNo(), nodeCd));
        }

        return new SetSlotNodeParam(
            slots
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
