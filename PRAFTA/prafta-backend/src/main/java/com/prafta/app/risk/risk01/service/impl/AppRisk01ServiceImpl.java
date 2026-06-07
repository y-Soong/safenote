package com.prafta.app.risk.risk01.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.risk.risk01.application.command.RiskAssessmentSaveCommand;
import com.prafta.app.risk.risk01.application.param.RiskAssessmentSaveParam;
import com.prafta.app.risk.risk01.application.param.RiskTypeInfoParam;
import com.prafta.app.risk.risk01.application.query.RiskTypeInfoQuery;
import com.prafta.app.risk.risk01.dto.response.RiskTypeInfoResponse;
import com.prafta.app.risk.risk01.mapper.AppRisk01Mapper;
import com.prafta.app.risk.risk01.result.RiskCategoryResult;
import com.prafta.app.risk.risk01.result.RiskHazardResult;
import com.prafta.app.risk.risk01.result.RiskTypeResult;
import com.prafta.app.risk.risk01.service.AppRisk01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.worktime.service.WorktimeGateService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-036-B2: 앱 위험성평가(risk01) 서비스 구현.
 *
 * <p>변경사항:
 *   <ul>
 *     <li>웹 컨벤션 DTO 플로우(Param -> Query/Command/Result -> Response) 적용.</li>
 *     <li>D-R2 NPE 해소: 단일 빌더 패턴 + 빈 List 정규화.</li>
 *     <li>D-R3 원인 보존: log.error + ApiException(COMMON_500_001) 전파.</li>
 *     <li>D-R4: @Transactional(rollbackFor = Exception.class) 명시 (파일+DB 부분 커밋 방지).</li>
 *     <li>token 캐노니컬라이즈 패턴 유지: cmpnyCd/userCd 는 모두 tokenInfo 출처.</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppRisk01ServiceImpl implements AppRisk01Service {

    private static final String FILE_TYPE_RISK_ASSESSMENT = "002"; // 002: 위험성평가

    private final AppRisk01Mapper appRisk01Mapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    /** prafta-app-022: 위험성발굴 등록 근무중 게이트(근무중에만 등록 허용). */
    private final WorktimeGateService worktimeGateService;

    @Override
    public RiskTypeInfoResponse selectRiskTypeInfo(RiskTypeInfoParam param) {

        TokenInfo tokenInfo = param.tokenInfo();
        RiskTypeInfoQuery query = RiskTypeInfoQuery.from(param);

        // 결과셋이 null 인 경우 빈 List 로 정규화 (D-R2: 단일 빌더 + NPE 방지).
        List<RiskCategoryResult> riskCategoryList = nullSafe(
                appRisk01Mapper.selectRiskCategory(query, tokenInfo)
        );
        List<RiskTypeResult> riskTypeList = nullSafe(
                appRisk01Mapper.selectRiskType(query, tokenInfo)
        );
        List<RiskHazardResult> riskHazardList = nullSafe(
                appRisk01Mapper.selectRiskHazard(query, tokenInfo)
        );

        // 응답 키 보존: riskCategoryList / riskTypeList / riskHazardList (FE Risk_01.vue 의존).
        return RiskTypeInfoResponse.builder()
                .riskCategoryList(riskCategoryList)
                .riskTypeList(riskTypeList)
                .riskHazardList(riskHazardList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRiskAssessments(RiskAssessmentSaveParam param) {

        // prafta-app-022: 근무중 게이트 — 파일 저장/UPSERT 진입 직전에 차단.
        //   미근무 시 WORKTIME_403_001(저장 미수행). 조회성 메서드(selectRiskTypeInfo)에는 미적용.
        worktimeGateService.assertWorking(param.tokenInfo());

        try {
            TokenInfo tokenInfo = param.tokenInfo();
            String cmpnyCd = tokenInfo.gv_cmpnyCd();
            String userCd = tokenInfo.gv_userCd();

            // 1) 파일 첨부가 있으면 fileMgmtCd 발급 + 저장.
            String fileMgmtCd = "";
            MultipartFile file = param.file();

            if (file != null && !file.isEmpty()) {

                fileMgmtCd = fileMapper.selectFileMgmtCd(
                        FileInfoQuery.from(cmpnyCd, FILE_TYPE_RISK_ASSESSMENT)
                );

                // prafta-036-C(H-3): param.siteCd() 는 Param.from 에서 token gv_siteCd 로 캐노니컬라이즈됨
                fileService.fileSave(FileInfoParam.from(
                        cmpnyCd
                        , userCd
                        , param.siteCd()
                        , FILE_TYPE_RISK_ASSESSMENT
                        , fileMgmtCd
                        , file
                ));
            }

            // 2) 위험성평가 UPSERT.
            appRisk01Mapper.mergeRiskAssessment(
                    RiskAssessmentSaveCommand.from(param, fileMgmtCd)
                    , tokenInfo
            );

        } catch (ApiException ae) {
            // 명시적 비즈니스 예외는 그대로 전파.
            throw ae;
        } catch (Exception e) {
            // D-R3: silent swallow 제거 + 원인 로깅 + 500 surface (트랜잭션 롤백 동반).
            log.error("[risk01] saveRiskAssessments 실패", e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    /**
     * MyBatis 결과셋 null 안전 정규화.
     */
    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
