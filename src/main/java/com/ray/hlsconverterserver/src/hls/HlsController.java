package com.ray.hlsconverterserver.src.hls;

import com.ray.hlsconverterserver.utils.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@Slf4j
public class HlsController {
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final FFmpegUtil ffmpegUtil;

    @Autowired
    public HlsController(FFmpeg ffmpeg, FFprobe ffprobe, FFmpegUtil ffmpegUtil) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.ffmpegUtil = ffmpegUtil;
    }

    @Value("${ffmpeg.upload.path}")
    private String UPLOAD_DIR;

    @GetMapping("/hls-make/{fileName}")
    @ResponseBody
    public void videoHlsMake(@PathVariable String fileName, Model model) throws IOException {
        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());

        final String FILEPATH = UPLOAD_DIR + "/" + fileName;
        final String ONLY_FILENAME = fileName.substring(0, fileName.lastIndexOf("."));
        final String TS_PATH = UPLOAD_DIR + "/" + ONLY_FILENAME;
        File tsPath = new File(TS_PATH);
        if (!tsPath.exists()) {
            tsPath.mkdir();
        }

        ffmpegUtil.getMediaInfo(FILEPATH);

        // TS 파일 생성
        FFmpegBuilder builder = new FFmpegBuilder()
                //.overrideOutputFiles(true) // 오버라이드 여부
                .setInput(FILEPATH) // 동영상파일
                .addOutput(TS_PATH + "/" + ONLY_FILENAME + ".m3u8") // 썸네일 경로
                .addExtraArgs("-profile:v", "baseline") //
                .addExtraArgs("-level", "3.0") //
                .addExtraArgs("-start_number", "0") //
                .addExtraArgs("-hls_time", "10") //
                .addExtraArgs("-hls_list_size", "0") //
                .addExtraArgs("-f", "hls") //
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();


        // 이미지 파일 생성
        FFmpegBuilder builderThumbNail = new FFmpegBuilder()
                .overrideOutputFiles(true) // 오버라이드 여부
                .setInput(FILEPATH) // 동영상파일
                .addExtraArgs("-ss", "00:00:03") // 썸네일 추출 시작점
                .addOutput(UPLOAD_DIR + "/" + ONLY_FILENAME + ".png") // 썸네일 경로
                .setFrames(1) // 프레임 수
                .done();
        FFmpegExecutor executorThumbNail = new FFmpegExecutor(ffmpeg, ffprobe);
        executorThumbNail.createJob(builderThumbNail).run();

        model.addAttribute("result", "OK");
    }


    @GetMapping("/hls")
    public String videoHls(Model model) {
        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());
        model.addAttribute("videoUrl", "/hls/video/video.m3u8");
        return "hls/hls";
    }

    @GetMapping("/hls/{fileName}")
    public String videoHlsByFilename(@PathVariable String fileName,
                                     Model model) {
        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());
        model.addAttribute("videoUrl", "/hls/"+fileName+"/"+fileName+".m3u8");
        return "hls/hls";
    }

    @GetMapping("/hls/{dirName}/{fileName}.m3u8")
    public ResponseEntity<Resource> videoHlsM3U8(@PathVariable String dirName,
                                                 @PathVariable String fileName) {
        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());
        String fileFullPath = UPLOAD_DIR + "/" + dirName + "/" + fileName + ".m3u8";
        Resource resource = new FileSystemResource(fileFullPath);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName + ".m3u8");
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }

    @GetMapping("/hls/{fileName}/{tsName}.ts")
    public ResponseEntity<Resource> videoHlsTs(@PathVariable String fileName, @PathVariable String tsName) {
        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());
        String fileFullPath = UPLOAD_DIR + "/" + fileName + "/" + tsName + ".ts";
        Resource resource = new FileSystemResource(fileFullPath);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + tsName + ".ts");
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }
}
