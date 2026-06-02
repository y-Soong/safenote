package com.prafta.app.chkLst.chkLst01.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.chkLst.chkLst01.application.command.InspectResultSaveCommand;
import com.prafta.app.chkLst.chkLst01.application.model.InspectAnswerItemModel;
import com.prafta.app.chkLst.chkLst01.application.param.ChecklistInfoParam;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.app.chkLst.chkLst01.application.query.ChecklistInfoQuery;
import com.prafta.app.chkLst.chkLst01.dto.response.ChecklistInfoResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.CheckpointContextResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.SaveInspectResultResponse;
import com.prafta.app.chkLst.chkLst01.mapper.AppChkLst01Mapper;
import com.prafta.app.chkLst.chkLst01.result.ChecklistInfoResult;
import com.prafta.app.chkLst.chkLst01.service.AppChkLst01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-036-B1: chkLst01 서비스 구현.
 * <p>prafta-app-011 변경사항:
 *   <ul>
 *     <li>siteCd 불일치(param.siteCdMismatch) -> siteNm 조회 후 403(CHKLST_403_001) 차단.</li>
 *     <li>체크포인트 미존재(결과 empty) -> 404(CHKLST_404_001).</li>
 *     <li>checklist-infos 응답에 checkpoint 컨텍스트 객체 추가.</li>
 *     <li>saveInspectResult: void -> SaveInspectResultResponse (요약 반환).</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppChkLst01ServiceImpl implements AppChkLst01Service {

    private static final Pattern FILE_KEY_PATTERN = Pattern.compile("^files\\[(.+)]$");
    private static final String FILE_TYPE_DAILY_INSPECT = "001"; // 001: 일일점검

    private final AppChkLst01Mapper appChkLst01Mapper;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    @Override
    public ChecklistInfoResponse selectChkLstInfo(ChecklistInfoParam param) {

        // prafta-app-011: siteCd 불일치 시 403 차단 (userSiteName 을 응답 JSON 독립 키로 반환)
        if (param.siteCdMismatch()) {
            String userSiteNm = appChkLst01Mapper.selectSiteNm(
                    param.gvCmpnyCd()
                    , param.siteCd()
            );
            String siteLabel = StringUtils.hasText(userSiteNm)
                    ? userSiteNm
                    : param.siteCd();
            log.warn("[chkLst01] siteCd 403 차단: req={}, token={}, siteNm={}, userCd={}",
                    param.reqSiteCd(), param.siteCd(), siteLabel, param.gvUserCd());
            throw ApiException.withExtra(
                    ChkLstErrorCode.CHKLST_403_001
                    , Map.of("userSiteName", siteLabel)
            );
        }

        TokenInfo tokenInfo = param.tokenInfo();

        List<ChecklistInfoResult> results = appChkLst01Mapper.selectChkLstInfo(
                ChecklistInfoQuery.from(param)
                , tokenInfo
        );

        // prafta-app-011: 체크포인트 미존재 -> 404
        if (results == null || results.isEmpty()) {
            log.warn("[chkLst01] 체크포인트 미존재: siteCd={}, chkptCd={}, userCd={}",
                    param.siteCd(), param.chkptCd(), param.gvUserCd());
            throw new ApiException(ChkLstErrorCode.CHKLST_404_001);
        }

        // prafta-app-011: 첫 row 에서 컨텍스트 추출 (모든 row 공통값)
        ChecklistInfoResult first = results.get(0);
        CheckpointContextResponse checkpoint = CheckpointContextResponse.builder()
                .chkptNm(first.chkptNm())
                .siteNm(first.siteNm())
                .chklstType(first.chklstType())
                .chkptDesc(first.chkptDesc())
                .totalCount(results.size())
                .build();

        return ChecklistInfoResponse.builder()
                .checklistInfos(results)
                .checkpoint(checkpoint)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaveInspectResultResponse saveInspectResult(InspectResultSaveParam param) {

        // prafta-app-011: siteCd 불일치 시 403 차단 (userSiteName 을 응답 JSON 독립 키로 반환)
        if (param.siteCdMismatch()) {
            String userSiteNm = appChkLst01Mapper.selectSiteNm(
                    param.tokenInfo().gv_cmpnyCd()
                    , param.siteCd()
            );
            String siteLabel = StringUtils.hasText(userSiteNm)
                    ? userSiteNm
                    : param.siteCd();
            log.warn("[chkLst01] save siteCd 403 차단: req={}, token={}, siteNm={}, userCd={}",
                    param.reqSiteCd(), param.siteCd(), siteLabel, param.tokenInfo().gv_userCd());
            throw ApiException.withExtra(
                    ChkLstErrorCode.CHKLST_403_001
                    , Map.of("userSiteName", siteLabel)
            );
        }

        try {
            // 1) multipart 'items' (JSON 묶음 파일) 파싱
            List<InspectAnswerItemModel> items = parseItems(param.items());

            // 2) 'files[ITEM_CD]' 패턴으로 들어온 이미지 파일을 itemCd 기준으로 인덱싱
            Map<String, MultipartFile> fileByItemCd = indexFilesByItemCd(param.files());

            TokenInfo tokenInfo = param.tokenInfo();
            String userCd = tokenInfo.gv_userCd();

            int okCount = 0;
            int badCount = 0;

            // 3) 항목별 처리: (이미지 있으면) 파일 저장 -> mergeChkptInspectAnswer
            for (InspectAnswerItemModel item : items) {

                String fileMgmtCd = "";
                MultipartFile img = fileByItemCd.get(item.itemCd());

                if (img != null && !img.isEmpty()) {
                    // 건별 fileMgmtCd 생성
                    fileMgmtCd = fileMapper.selectFileMgmtCd(
                            FileInfoQuery.from(param.cmpnyCd(), FILE_TYPE_DAILY_INSPECT)
                    );

                    // prafta-036-C(H-3): param.siteCd() 는 Param.from 에서 token gv_siteCd 로 캐노니컬라이즈됨
                    fileService.fileSave(FileInfoParam.from(
                            param.cmpnyCd()
                            , userCd
                            , param.siteCd()
                            , FILE_TYPE_DAILY_INSPECT
                            , fileMgmtCd
                            , img
                    ));
                }

                appChkLst01Mapper.mergeChkptInspectAnswer(
                        InspectResultSaveCommand.from(param, item, fileMgmtCd)
                        , tokenInfo
                );

                // prafta-app-011: 양호/불량 집계
                if ("Y".equals(item.inspectValue())) {
                    okCount++;
                } else if ("N".equals(item.inspectValue())) {
                    badCount++;
                }
            }

            // prafta-app-011: 체크포인트명 조회 (화면 C 요약 표시용)
            String chkptName = resolveChkptName(param);

            return SaveInspectResultResponse.builder()
                    .chkptName(chkptName)
                    .okCount(okCount)
                    .badCount(badCount)
                    .savedCount(items.size())
                    .workDate(param.workDate())
                    .build();

        } catch (ApiException ae) {
            // 명시적 비즈니스 예외는 그대로 전파
            throw ae;
        } catch (Exception e) {
            // 기존 silent swallow 제거: 실패 시 500 으로 surface (트랜잭션 롤백 동반)
            log.error("[chkLst01] saveInspectResult 실패", e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    /**
     * 저장 완료 응답에 사용할 체크포인트명 조회.
     * <p>chkptCd + siteCd 로 TB_CHKPT_TYPE_MGMT 를 다시 조회하는 대신,
     *   selectSiteNm 은 이미 있으므로 단독 CHKPT_NM 조회는 selectChkLstInfo 결과 재사용.
     *   저장 플로우에서 조회 비용을 최소화하기 위해 별도 쿼리 없이 param 에서 가져올 수 없는 경우
     *   빈 문자열 폴백으로 처리한다(저장 자체는 이미 완료).
     */
    private String resolveChkptName(InspectResultSaveParam param) {
        try {
            ChecklistInfoQuery query = new ChecklistInfoQuery(
                    param.cmpnyCd()
                    , param.siteCd()
                    , param.chkptCd()
                    , null
            );
            List<ChecklistInfoResult> rows = appChkLst01Mapper.selectChkLstInfo(query, param.tokenInfo());
            if (rows != null && !rows.isEmpty() && StringUtils.hasText(rows.get(0).chkptNm())) {
                return rows.get(0).chkptNm();
            }
        } catch (Exception e) {
            log.warn("[chkLst01] chkptName 조회 실패 (저장은 완료): {}", e.getMessage());
        }
        return "";
    }

    /**
     * multipart 'items' (JSON 배열) 파싱.
     * @param itemsFile MultipartFile (null/empty 허용)
     * @return 파싱된 항목 모델 목록 (비어있을 수 있음)
     */
    private List<InspectAnswerItemModel> parseItems(MultipartFile itemsFile) throws java.io.IOException {

        List<InspectAnswerItemModel> result = new ArrayList<>();

        if (itemsFile == null || itemsFile.isEmpty()) {
            return result;
        }

        String itemsJson = new String(itemsFile.getBytes(), StandardCharsets.UTF_8);
        if (itemsJson.isEmpty()) {
            return result;
        }

        JsonNode node = objectMapper.readTree(itemsJson);
        if (node.isArray()) {
            for (JsonNode n : node) {
                result.add(objectMapper.treeToValue(n, InspectAnswerItemModel.class));
            }
        }
        return result;
    }

    /**
     * 'files[ITEM_CD]' 키 패턴에서 ITEM_CD 추출하여 인덱싱.
     */
    private Map<String, MultipartFile> indexFilesByItemCd(Map<String, MultipartFile> files) {

        Map<String, MultipartFile> fileByItemCd = new HashMap<>();
        if (files == null) {
            return fileByItemCd;
        }

        for (Map.Entry<String, MultipartFile> e : files.entrySet()) {
            Matcher m = FILE_KEY_PATTERN.matcher(e.getKey());
            if (m.find()) {
                fileByItemCd.put(m.group(1), e.getValue());
            }
        }
        return fileByItemCd;
    }
}
