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
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service @Slf4j
public class HlsService {
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final FFmpegUtil ffmpegUtil;

    @Autowired
    public HlsService(FFmpeg ffmpeg, FFprobe ffprobe, FFmpegUtil ffmpegUtil) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.ffmpegUtil = ffmpegUtil;
    }


    @Value("${ffmpeg.upload.path}")
    private String UPLOAD_DIR;

    public void hlsMakeABS(String fileName, Model model) throws IOException {
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
        FFmpegOutputBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true) // 오버라이드 여부
//                .setVerbosity(FFmpegBuilder.Verbosity.DEBUG)
                .setInput(FILEPATH) // 동영상파일
                .addOutput(TS_PATH + "/" + "stream_%v" + ".m3u8") // 썸네일 경로
                .addExtraArgs("-preset", "veryfast")

                .addExtraArgs("-keyint_min", "100")
                .addExtraArgs("-g", "100")
                .addExtraArgs("-sc_threshold", "0")
                .addExtraArgs("-r", "25")
                .addExtraArgs("-c:v", "libx264")
                .addExtraArgs("-pix_fmt", "yuv420p")
                .addExtraArgs("-crf", "21");

        List<String> videoSizeList = new ArrayList<>(Arrays.asList(
                "640x360",
                "854x480",
                "1280x720",
                "1920x1080"
        ));
        List<String> videoRateList = new ArrayList<>(Arrays.asList(
                "365k", "1.1M", "4.5M", "7.8M"
        ));
        List<String> videoBuffList = new ArrayList<>(Arrays.asList(
                "730k", "2.2M", "9M", "15.6M"
        ));


        for (int i = 0; i < 4; i++) {
            builder.addExtraArgs("-map", "0:v")
                    .addExtraArgs("-s:v:" + i, videoSizeList.get(i))
                    .addExtraArgs("-maxrate:v:" + i, videoRateList.get(i))
                    .addExtraArgs("-bufsize:v:"+i, videoBuffList.get(i));

        }
        for (int i = 0; i < 4; i++)
            builder.addExtraArgs("-map","0:a");
        builder
                .addExtraArgs("-c:a","aac")
                .addExtraArgs("-b:a","128k")
                .addExtraArgs("-ac","1")
                .addExtraArgs("-ar","44100")
                .addExtraArgs("-f","hls")
                .addExtraArgs("-hls_time", "4" )
                .addExtraArgs("-hls_playlist_type","vod")
                .addExtraArgs("-master_pl_name", ONLY_FILENAME + ".m3u8");

        StringBuilder mappingSb = new StringBuilder().append("\"");
        for (int i = 0; i < 4; i++) {
            if (i==0) mappingSb.append("v:"+i+",a:"+i);
            else mappingSb.append(" v:"+i+",a:"+i);
        }
        mappingSb.append("\"");
        builder.addExtraArgs("-var_stream_map", mappingSb.toString());
        FFmpegBuilder ffmpegBuilder = builder.done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(ffmpegBuilder).run();


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
}
