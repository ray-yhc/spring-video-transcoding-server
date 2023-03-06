package com.ray.hlsconverterserver.src.transcoding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
@Slf4j
public class TransController {
    public final TransService transService;

//    @Autowired
//    public TransController(TransService transService) {
//        this.transService = transService;
//    }


    /**
     * S3에 업로드된 영상 변환 요청
     *
     * 파일명
     * -> 파일 다운로드하기
     * -> ffmpeg 실행하여 변환하기
     * -> ts, m3u8 파일 S3 업로드
     * -> m3u8 마스터 파일 만들기 or 수정하기
     * -> 결과 반환
     *
     *
     */
    @PostMapping("/convert")
    public ResponseEntity<Integer> videoConvertRequest(
            @RequestParam String name,
            @RequestParam Integer resolution
    ) {

        transService.convertRequest(name, resolution);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
