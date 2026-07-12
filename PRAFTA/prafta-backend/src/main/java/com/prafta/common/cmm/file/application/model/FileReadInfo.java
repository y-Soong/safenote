package com.prafta.common.cmm.file.application.model;

/**
 * tb_file_info 원본 read 조회 결과.
 *
 * <p>⚠️ MyBatis record 위치매핑(argNameBasedConstructorAutoMapping=false):
 *    생성자 인자 순서 = SELECT 컬럼 순서(FILE_PATH, FILE_NM, FILE_EXT, FILE_TYPE)와 정확히 일치해야 한다.
 */
public record FileReadInfo(
    String filePath     // FILE_PATH (공개/DB 저장용 상대경로, 예: /uploads/{cmpny}/{yyyymmdd}/{site}/{fileType})
    , String fileNm     // FILE_NM (원본 파일명)
    , String fileExt    // FILE_EXT (확장자, 점 제외)
    , String fileType   // FILE_TYPE [SYS010] (위험성평가='002')
) {}
