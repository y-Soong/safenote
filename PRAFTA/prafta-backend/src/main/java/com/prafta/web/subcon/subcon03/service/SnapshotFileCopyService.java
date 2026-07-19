package com.prafta.web.subcon.subcon03.service;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공유 스냅샷 첨부 복제 서비스(PRAFTA-SUBCON-T7 §5-3) — T6 {@code copyPhotoForTier} 의 일반화.
 *
 * <p>원본 첨부 바이트를 읽어 <b>수신 테넌트 소유의 신규 tb_file_info 레코드</b>로 재저장한다.
 *    제공측 원본 삭제와 무관하게 수신분이 보존된다. 위험성평가/아차사고 첨부는 이미지에 한정되지 않으므로
 *    (메인 세션 Q4) 범용 로더 {@link FileService#loadFileBytes} 로 <b>전 파일타입</b>을 확장자 보존 복제한다.
 *
 * <p><b>실패는 비치명(D6)</b>: 원본 없음/읽기 실패/저장 실패 시 빈 문자열을 반환하고(경고 로그) 상세행 저장은
 *    막지 않는다. 파일 한 건 때문에 공유 전체를 실패시키지 않는다. 물리 I/O 는 승인 트랜잭션 내부에서 수행하되
 *    전체 롤백 시 디스크 고아파일은 허용한다(현행 fileSave 특성 동일).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotFileCopyService {

    private final FileService fileService;
    private final FileMapper fileMapper;

    /**
     * 원본 첨부를 수신사 소유 신규 파일로 복제한다.
     *
     * @param srcCmpnyCd     원본 소유 회사코드(제공사, 또는 릴레이 시 하위 수신 보유 회사 = 승인자 자신)
     * @param srcFileMgmtCd  원본 파일코드(공백/null 이면 첨부 없음)
     * @param dstCmpnyCd     복제 대상(수신) 회사코드 = 요청자 회사(OWNER_CMPNY_CD)
     * @param dstSiteCd      복제 대상 사업장코드 = 요청자 사업장(REQ_SITE_CD)
     * @param fileType       파일 유형[SYS010] — 위험성평가 '002' / 아차사고 '004'
     * @return 신규 수신사 소유 FILE_MGMT_CD(첨부 없음/복제 실패 시 빈 문자열)
     */
    public String copyFileForOwner(String srcCmpnyCd, String srcFileMgmtCd,
            String dstCmpnyCd, String dstSiteCd, String fileType) {

        if (srcFileMgmtCd == null || srcFileMgmtCd.isBlank()) {
            return "";
        }

        try {
            FileBytesResult file = fileService.loadFileBytes(new FileReadQuery(srcCmpnyCd, srcFileMgmtCd));
            if (file == null || file.data() == null || file.data().length == 0) {
                log.warn("스냅샷 첨부 복제 생략(원본 없음) - src={}:{}, dst={}, fileType={}",
                        srcCmpnyCd, srcFileMgmtCd, dstCmpnyCd, fileType);
                return "";
            }

            String ext = file.fileExt();
            if (ext == null || ext.isBlank()) {
                log.warn("스냅샷 첨부 복제 생략(확장자 없음) - src={}:{}, dst={}", srcCmpnyCd, srcFileMgmtCd, dstCmpnyCd);
                return "";
            }

            String newFileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(dstCmpnyCd, fileType));

            fileService.fileSave(FileInfoParam.from(
                    dstCmpnyCd
                    , "SYSTEM"
                    , dstSiteCd
                    , fileType
                    , newFileMgmtCd
                    , new BytesMultipartFile("file", newFileMgmtCd + "." + ext, file.contentType(), file.data())));

            return newFileMgmtCd;

        } catch (Exception e) {
            // 첨부 복제 실패는 스냅샷 생성 자체를 막지 않는다(첨부 없이 상세행 저장 — 실패 로그만).
            log.error("스냅샷 첨부 복제 실패 - src={}:{}, dst={}:{}, fileType={}, 원인={}",
                    srcCmpnyCd, srcFileMgmtCd, dstCmpnyCd, dstSiteCd, fileType, e.getMessage());
            return "";
        }
    }
}
