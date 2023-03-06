package com.ray.hlsconverterserver.src.awsS3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.ray.hlsconverterserver.utils.MultipartUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3ResourceStorage {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3Client amazonS3Client;

    public void store(String DirPath, String fileName, MultipartFile multipartFile) {
        log.debug("DirPath = {}", DirPath);
        log.debug("fullPath = {}", fileName);
        File file = new File(MultipartUtil.getLocalHomeDirectory(), fileName);
        try {
            multipartFile.transferTo(file);
            // s3 내에 dir 생성
            amazonS3Client.putObject(bucket, DirPath, new ByteArrayInputStream(new byte[0]), new ObjectMetadata() );
            // s3에 파일 업로드
            amazonS3Client.putObject(new PutObjectRequest(bucket, DirPath + fileName, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException();
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public ResponseEntity<byte[]> getObject(String storedFileName) throws IOException {
        S3Object o = amazonS3Client.getObject(new GetObjectRequest(bucket, storedFileName));
        S3ObjectInputStream objectInputStream = o.getObjectContent();
        byte[] bytes = IOUtils.toByteArray(objectInputStream);

        String fileName = URLEncoder.encode(storedFileName, "UTF-8").replaceAll("\\+", "%20");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        httpHeaders.setContentLength(bytes.length);
        httpHeaders.setContentDispositionFormData("attachment", fileName);

        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);
    }



    public File saveObjectInLocal (String storedFileName, String directoryName) {
        try {
            S3Object o = amazonS3Client.getObject(new GetObjectRequest(bucket, storedFileName));
            S3ObjectInputStream objectContent = o.getObjectContent();
            FileCopyUtils.copy(objectContent,
                    new FileOutputStream(
                            directoryName
                                    + '/' + storedFileName
                    ));
        } catch (AmazonS3Exception ae) {
            ae.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }

        return new File(directoryName
                + '/' + storedFileName);
    }
}
