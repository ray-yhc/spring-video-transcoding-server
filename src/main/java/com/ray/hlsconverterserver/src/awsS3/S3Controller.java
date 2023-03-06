package com.ray.hlsconverterserver.src.awsS3;

import com.ray.hlsconverterserver.src.awsS3.model.FileDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor@Slf4j
public class S3Controller {
    private final S3Service s3Service;

    /**
     * 요청한 파일을 S3에 업로드한다. 업로드된 오브젝트의 정보 반환한다.
     */
    @PostMapping(value = "/upload", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FileDetail> uploadFile(
            @RequestPart("file") MultipartFile multipartFile) {
        return ResponseEntity.ok(s3Service.save(multipartFile));
    }

    /**
     * S3에서 파일을 다운로드한다.
     * fileName : 파일의 경로 / 이름
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String fileName) throws IOException {
        log.info("fileName = {}", fileName);
        return s3Service.getObject(fileName);
    }
}
