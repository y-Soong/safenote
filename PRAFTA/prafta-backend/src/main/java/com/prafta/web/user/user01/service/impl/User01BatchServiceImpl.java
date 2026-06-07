package com.prafta.web.user.user01.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.application.param.UserInfoParam;
import com.prafta.web.user.user01.dto.UserBatchUpdateResponse;
import com.prafta.web.user.user01.dto.UserUpdateFailItem;
import com.prafta.web.user.user01.service.User01BatchService;
import com.prafta.web.user.user01.service.User01Service;
import com.prafta.web.user.user01.util.UserExcelRowParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User01BatchServiceImpl implements User01BatchService {

    private final User01Service user01Service;

    // 엑셀 업로드 제한 (D7 보안 가드).
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int MAX_DATA_ROWS = 1000;

    @Override
    public UserBatchUpdateResponse updateUserInfoBatch(UserInfoParam param) {

        // prafta-019-B 보안 하드닝: 사용자 정보(직급/권한/소속 포함) 저장은 매니저 권한 필요.
        // (형제 update-user-credit / update-user-hire-date 와 동일 가드. 회사 스코프는 Param.from에서 토큰 강제)
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("사용자 정보 저장 권한 부족 - userCd={}, authCd={}", param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(UserErrorCode.USER_403_001);
        }

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
                        e.getResolvedMessage(),
                        null   // prafta-052: 그리드 경로는 양식 원본 행 없음(단일 시트 유지)
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
                        "처리 중 오류가 발생했습니다.\n관리자에게 문의해주세요.",
                        null   // prafta-052: 그리드 경로는 양식 원본 행 없음(단일 시트 유지)
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

    /**
     * PRAFTA-036 — 엑셀 업로드로 들어온 사용자 생성 요청을 행별로 처리한다.
     *
     * <p>각 행은 {@link User01Service#insertUserOne(UserCreateParam)} 의 REQUIRES_NEW 트랜잭션으로
     * 격리되어, 한 행의 검증/INSERT 실패가 다른 행의 처리에 영향을 주지 않는다.
     * 실패 사유는 {@link UserErrorCode} 의 표준 한글 메시지(D6)로 응답한다.
     */
    @Override
    public UserBatchUpdateResponse insertUserBatch(List<UserCreateParam> params) {

        if (params == null) {
            return new UserBatchUpdateResponse(true, 0, 0, 0, List.of());
        }

        // 권한 가드: 단건 endpoint 와 동일 — master / hr 만.
        if (!params.isEmpty()
                && !AuthRoleUtils.isManager(params.get(0).gvAuthCd())) {
            log.warn("엑셀 일괄 생성 권한 부족 - 요청자={}, authCd={}",
                    params.get(0).gvUserCd(), params.get(0).gvAuthCd());
            throw new ApiException(UserErrorCode.USER_403_001);
        }

        List<UserUpdateFailItem> fails = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < params.size(); i++) {
            UserCreateParam p = params.get(i);
            try {
                user01Service.insertUserOne(p); // REQUIRES_NEW 트랜잭션
                successCount++;

            } catch (ApiException e) {
                ApiErrorCode code = e.getErrorCode();
                fails.add(new UserUpdateFailItem(
                        i,
                        p.userId(),
                        code.code(),
                        e.getResolvedMessage(),
                        UserExcelRowParser.toSourceRow(p)   // prafta-052: 실패 행 재업로드용 원본값
                ));

            } catch (Exception e) {
                log.error("엑셀 일괄 생성 행 처리 실패 - index={}, userId={}", i, p.userId(), e);
                fails.add(new UserUpdateFailItem(
                        i,
                        p.userId(),
                        CommonErrorCode.COMMON_500_001.code(),
                        "처리 중 오류가 발생했습니다.\n관리자에게 문의해주세요.",
                        UserExcelRowParser.toSourceRow(p)   // prafta-052: 실패 행 재업로드용 원본값
                ));
            }
        }

        int total = params.size();
        int failCount = fails.size();

        return new UserBatchUpdateResponse(
                failCount == 0,
                total,
                successCount,
                failCount,
                fails
        );
    }

    /**
     * PRAFTA-036 — 엑셀(.xlsx) 업로드 처리. 파일 검증 → 파싱 → 행별 생성 → 결과 응답.
     * 파일 검증/파싱 실패는 ApiException(범용 4xx)로, 행별 검증 실패는 응답 body 의 fails 로 수집한다(D6).
     */
    @Override
    public UserBatchUpdateResponse uploadUserCreates(MultipartFile file, TokenInfo tokenInfo) {

        // 권한 가드 (단건 endpoint 와 동일).
        if (tokenInfo == null
                || tokenInfo.gv_authCd() == null
                || !AuthRoleUtils.isManager(tokenInfo.gv_authCd())) {
            throw new ApiException(UserErrorCode.USER_403_001);
        }

        // 1) 파일 존재 / 형식 / 크기 검증.
        if (file == null || file.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".xlsx")) {
            throw new ApiException(UserErrorCode.USER_400_050);
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(UserErrorCode.USER_400_051);
        }

        // 2) POI 로 시트 0 파싱.
        List<UserCreateParam> params;
        try (InputStream in = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(UserErrorCode.USER_400_053);
            }
            Sheet sheet = workbook.getSheetAt(0);
            params = UserExcelRowParser.parse(sheet, tokenInfo);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("엑셀 업로드 파싱 실패 - file={}, size={}", originalName, file.getSize(), e);
            throw new ApiException(UserErrorCode.USER_400_053);
        }

        // 3) 데이터 행 수 검증.
        if (params.size() > MAX_DATA_ROWS) {
            throw new ApiException(UserErrorCode.USER_400_052);
        }

        // 4) 행별 INSERT 위임 (insertUserBatch — REQUIRES_NEW).
        UserBatchUpdateResponse result = insertUserBatch(params);

        log.info("엑셀 업로드 처리 완료 - 요청자={}, 파일={}, 전체={}, 성공={}, 실패={}",
                tokenInfo.gv_userCd(), originalName,
                result.totalCount(), result.successCount(), result.failCount());

        return result;
    }
}
