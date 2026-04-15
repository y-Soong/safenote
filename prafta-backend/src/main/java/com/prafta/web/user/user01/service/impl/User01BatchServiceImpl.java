package com.prafta.web.user.user01.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.UserInfoParam;
import com.prafta.web.user.user01.dto.UserBatchUpdateResponse;
import com.prafta.web.user.user01.dto.UserUpdateFailItem;
import com.prafta.web.user.user01.service.User01BatchService;
import com.prafta.web.user.user01.service.User01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User01BatchServiceImpl implements User01BatchService {

    private final User01Service user01Service;

    @Override
    public UserBatchUpdateResponse updateUserInfoBatch(UserInfoParam param) {

        List<UserUpdateFailItem> fails = new ArrayList<>();
        int successCount = 0;

//        for(UserInfoModel model : param.userInfoModelList()) {
        for (int i = 0; i < param.userInfoModelList().size(); i++) {
        	UserInfoModel model = param.userInfoModelList().get(i);

            try {
                user01Service.updateOneUserInfo(model); // REQUIRES_NEW 트랜잭션
                successCount++;

            } catch (ApiException e) {
                // 비즈니스/검증 실패: 에러코드 포함해서 수집
                fails.add(new UserUpdateFailItem(
                        i,
                        model.userId(),
                        e.getErrorCode().code(),
                        e.getResolvedMessage()
                ));

                // 정책 1) 실패해도 계속 진행
                 continue;

                // 정책 2) 첫 실패에서 중단(원하면 이걸로)
                // break;

            } catch (Exception e) {
                // 예상 못한 예외는 내부 로그만 남기고, 응답은 안전하게
                log.error("Batch update failed. index={}, userId={}", i, model.userId(), e);

                fails.add(new UserUpdateFailItem(
                        i,
                        model.userId(),
                        CommonErrorCode.COMMON_500_001.code(),
                        "처리 중 오류가 발생했습니다.\n관리자에게 문의해주세요."
                ));

                // continue;  // 계속 진행
                // break;     // 또는 중단
            }
        }

        int total = param.userInfoModelList().size();
        int failCount = fails.size();

        return new UserBatchUpdateResponse(
                failCount == 0,
                total,
                successCount,
                failCount,
                fails
        );
    }
}