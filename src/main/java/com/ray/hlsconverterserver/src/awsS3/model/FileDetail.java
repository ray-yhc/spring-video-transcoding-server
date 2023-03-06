package com.ray.hlsconverterserver.src.awsS3.model;

import com.ray.hlsconverterserver.utils.MultipartUtil;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class FileDetail {
    private String id;
    private String name;
    private String format;
    private String path;
    private long bytes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public static FileDetail multipartOf(MultipartFile multipartFile) {
        final String fileId = MultipartUtil.createFileId();
        final String format = MultipartUtil.getFormatByName(multipartFile.getOriginalFilename());
        return FileDetail.builder()
                .id(fileId)
                .name(multipartFile.getOriginalFilename())
                .format(format)
                .path(MultipartUtil.createDirPath(fileId) + MultipartUtil.createName(fileId, format))
                .bytes(multipartFile.getSize())
                .build();
    }

    public String getSaveDirPath() {
        return MultipartUtil.createDirPath(id);
    }
    public String getSaveFileName() {
        return MultipartUtil.createName(id, format);
    }
}



