package com.ray.hlsconverterserver.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
public final class MultipartUtil {

//    @Value("${cloud.aws.s3.base-dir}")
    private static String BASE_DIR = "video";

    /**
     * 로컬에서의 사용자 홈 디렉토리 경로를 반환합니다.
     */
    public static String getLocalHomeDirectory() {
        return System.getProperty("user.home");
    }

    /**
     * 새로운 파일 고유 ID를 생성합니다.
     * @return 36자리의 UUID
     */
    public static String createFileId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Multipart 의 ContentType 값에서 / 이후 확장자만 잘라냅니다.
     * @param contentType ex) image/png
     * @return ex) png
     */
    public static String getFormat(String contentType) {
        if (StringUtils.hasText(contentType)) {
            return contentType.substring(contentType.lastIndexOf('/') + 1);
        }
        return null;
    }

    /**
     * Multipart 의 OriginalFilename 값에서 . 이후 확장자만 잘라냅니다.
     * @param OriginalFilename ex) myFile.png
     * @return ex) png
     */
    public static String getFormatByName(String OriginalFilename) {
        if (StringUtils.hasText(OriginalFilename)) {
            return OriginalFilename.substring(OriginalFilename.lastIndexOf('.') + 1);
        }
        return null;
    }

    /**
     * 파일의 전체 경로를 생성합니다.
     * @param fileId 생성된 파일 고유 ID
     * @param format 확장자
     */
    public static String createName(String fileId, String format) {
        return String.format("%s.%s", fileId, format);
    }

    /**
     * 파일의 디렉토리 경로를 생성합니다.
     * @param fileId 생성된 파일 고유 ID
     */
    public static String createDirPath(String fileId) {
        log.debug("BASE_DIR = {}", BASE_DIR);
        return String.format("%s/%s/", BASE_DIR, fileId);
    }
}