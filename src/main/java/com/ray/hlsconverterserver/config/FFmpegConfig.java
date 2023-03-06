package com.ray.hlsconverterserver.config;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.io.IOException;

@Slf4j
@Configuration
public class FFmpegConfig {
    @Value("${ffmpeg.location}")
    private String ffmpegLocation;
    @Value("${ffprobe.location}")
    private String ffprobeLocation;

    @Bean(name = "ffmpeg")
    public FFmpeg ffmpeg() throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegLocation);
        Assert.isTrue(ffmpeg.isFFmpeg(), "failed to import ffmpeg");
        return ffmpeg;
    }

    @Bean(name = "ffprobe")
    public FFprobe ffprobe() throws IOException {
        FFprobe ffprobe = new FFprobe(ffprobeLocation);
        Assert.isTrue(ffprobe.isFFprobe(), "failed to import ffprobe");
        return ffprobe;
    }
}

