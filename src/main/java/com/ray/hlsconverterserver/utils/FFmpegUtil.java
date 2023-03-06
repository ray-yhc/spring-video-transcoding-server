package com.ray.hlsconverterserver.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class FFmpegUtil {
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    private final ObjectMapper objectMapper;

    @Autowired
    public FFmpegUtil(FFmpeg ffmpeg, FFprobe ffprobe, ObjectMapper objectMapper) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.objectMapper = objectMapper;
    }

    @Value("ffmpeg.upload.path")
    private String convertSavePath;

    /**
     * get Media Info
     * @param filePath
     * @return
     * @throws IOException
     */
    public FFmpegProbeResult getMediaInfo(String filePath) throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe(filePath);

        if(log.isDebugEnabled()){
            log.debug("========== VideoFileUtils.getMediaInfo() ==========");
            log.debug("filename : {}", probeResult.getFormat().filename);
            log.debug("format_name : {}", probeResult.getFormat().format_name);
            log.debug("format_long_name : {}", probeResult.getFormat().format_long_name);
            log.debug("tags : {}", probeResult.getFormat().tags.toString());
            log.debug("duration : {} second", probeResult.getFormat().duration);
            log.debug("size : {} byte", probeResult.getFormat().size);

            log.debug("width : {} px", probeResult.getStreams().get(0).width);
            log.debug("height : {} px", probeResult.getStreams().get(0).height);
            log.debug("===================================================");
        }
        return probeResult;
    }

    /**
     * 비디오 Prop을 변경시키는 로직
     * @param probeResult
     * @param format 영상 포맷 (확장자 ex - mp4, mpeg )
     * @param codec 비디오 코덱
     * @param audioChannel 오디오 채널 ( 1: 모노, 2: 스테레오 )
     * @param width 해상도 ( 너비 )
     * @param height 해상도 ( 높이 )
     * @return 변환 결과
     */
    public boolean convertVideoProp(FFmpegProbeResult probeResult, String format, String codec, int audioChannel, int width, int height) {
        boolean result = false;

        FFmpegBuilder builder = new FFmpegBuilder().setInput(probeResult)
                .overrideOutputFiles(true)
                .addOutput(convertSavePath + "/temp."+format)
                .setFormat(format)
                .setVideoCodec(codec)
                .setAudioChannels(audioChannel)
                .setVideoResolution(width, height)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

//        StopWatch stopWatch = new StopWatch("convertVideoTimer");

        FFmpegExecutor executable = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executable.createJob(builder);
        job.run();

        if (job.getState() == FFmpegJob.State.FINISHED) {
            result = true;
        }

        return result;
    }

    public void createThumbnail(String filePath, String thumbnailPath){
        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true) // 오버라이드 여부
                .setInput(filePath) // 썸네일 생성대상 파일
                .addExtraArgs("-ss", "00:00:05") // 썸네일 추출 시작점
                .addOutput(thumbnailPath) // 썸네일 파일의 Path
                .setFrames(100) // 프레임 수
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();
    }
}
