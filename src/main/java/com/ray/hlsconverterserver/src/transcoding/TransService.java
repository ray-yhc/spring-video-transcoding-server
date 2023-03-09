package com.ray.hlsconverterserver.src.transcoding;

import com.ray.hlsconverterserver.src.awsS3.S3ResourceStorage;
import com.ray.hlsconverterserver.src.transcoding.enums.VideoResolution;
import com.ray.hlsconverterserver.utils.FFmpegUtil;
import com.ray.hlsconverterserver.utils.MultipartUtil;
import lombok.RequiredArgsConstructor;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransService {
    private final S3ResourceStorage s3Storage;
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final FFmpegUtil ffmpegUtil;

//    @Autowired
//    public TransService(S3ResourceStorage s3Storage) {
//        this.s3Storage = s3Storage;
//    }

    public void convertRequest(String s3FilePath, int resolution) {

        //파일명
        // aaa/aaa/bbb.xxx => bbb
        String onlyName = s3FilePath.substring(0, s3FilePath.lastIndexOf("."));
        if (onlyName.contains("/")) onlyName = onlyName.substring(onlyName.lastIndexOf("/") + 1);

        String nameExtention = s3FilePath;
        if (nameExtention.contains("/")) nameExtention = nameExtention.substring(nameExtention.lastIndexOf("/") + 1);
        String s3DirPath =  s3FilePath.substring(0, s3FilePath.lastIndexOf("/")) + "/";

        // bbb 디렉토리 추가
        String rootDirPath = MultipartUtil.getLocalHomeDirectory();
        String dirPath = rootDirPath + "/" + s3FilePath.substring(0, s3FilePath.lastIndexOf("/"));
        File fileDir = new File(dirPath);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }


        log.debug("================================");
        log.debug("s3FilePath = {}", s3FilePath);
        log.debug("onlyName = {}", onlyName);
        log.debug("s3DirPath = {}", s3DirPath);
        log.debug("rootDirPath = {}", rootDirPath);
        log.debug("dirPath = {}", dirPath);
        log.debug("================================");

        try {
            //1. 파일 다운로드하기
            File savedVideo = s3Storage.saveObjectInLocal(s3FilePath, rootDirPath);
            //2.  ffmpeg 실행하여 변환하기
            for (VideoResolution r : VideoResolution.values()){
                convertFFmpegVideo(s3FilePath, r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //3. m3u8 마스터 파일 만들기 or 수정하기
            createMasterFile(onlyName, dirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 4. ts, m3u8 파일 S3 업로드
        try {
            // mp4 지우기
            File[] folderList = fileDir.listFiles(); //파일리스트 얻어오기

            for (int j = 0; j < Objects.requireNonNull(folderList).length; j++) {
                if (folderList[j].getName() == nameExtention) // 영상파일의 경우
                    continue;
                else {
                    s3Storage.store(s3DirPath, folderList[j].getName(), folderList[j]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        // 작업 파일 지우기
        while (fileDir.exists()) {
            File[] folder_list = fileDir.listFiles(); //파일리스트 얻어오기

            for (int j = 0; j < Objects.requireNonNull(folder_list).length; j++)
                folder_list[j].delete(); //파일 삭제

            if (folder_list.length == 0 && fileDir.isDirectory())
                fileDir.delete();
        }


    }

    public File createMasterFile(String onlyFileName, String dirName ) throws IOException {
        log.debug("onlyFileName = {}", onlyFileName);
        log.debug("dirName = {}", dirName);
        File masterFile = new File( dirName, onlyFileName+".m3u8");
        if(!masterFile.exists())
            masterFile.createNewFile();

        log.debug("masterFile.getAbsolutePath() = {}", masterFile.getAbsolutePath());

        // BufferedWriter 생성
        BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile, true));

        // 파일에 쓰기
        writer.write("#EXTM3U");
        writer.write("#EXT-X-VERSION:3");
        writer.newLine();

        for (VideoResolution r : VideoResolution.values()){
            writer.write("#EXT-X-STREAM-INF:BANDWIDTH=35200,RESOLUTION=");
            writer.write(r.resolution());
            writer.newLine();
            writer.write("stream_" + r.label() + ".m3u8");
            writer.newLine();
        }

        // 버퍼 및 스트림 뒷정리
        writer.flush(); // 버퍼의 남은 데이터를 모두 쓰기
        writer.close(); // 스트림 종료

        return masterFile;
    }



    public void convertFFmpegVideo(String fileName, VideoResolution resolution) throws IOException {
        String ROOT_DIR = MultipartUtil.getLocalHomeDirectory();

        log.debug("************** class = {}, function = {}", this.getClass().getName(), new Object() {
        }.getClass().getEnclosingMethod().getName());

        final String FILEPATH = ROOT_DIR + "/" + fileName;
        final String ONLY_FILENAME = fileName.substring(0, fileName.lastIndexOf("."));
        final String TS_PATH = ROOT_DIR + "/" + fileName.substring(0, fileName.lastIndexOf("/"));
        File tsPath = new File(TS_PATH);
        if (!tsPath.exists()) {
            tsPath.mkdir();
        }

        log.debug("ONLY_FILENAME = {}", ONLY_FILENAME);

        ffmpegUtil.getMediaInfo(FILEPATH);

        // TS 파일 생성
        FFmpegOutputBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true) // 오버라이드 여부
                // .setVerbosity(FFmpegBuilder.Verbosity.DEBUG)
                .setInput(FILEPATH) // 동영상파일
                // 썸네일 경로
                .addOutput(TS_PATH + "/stream_" + resolution.label() + ".m3u8")
                .addExtraArgs("-profile:v", "baseline")
                .addExtraArgs("-level", "3.0");
        // 해상도 옵션 입력
        builder.addExtraArgs("-s", resolution.resolution());
        // hls 옵션
        builder.addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "5")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-f", "hls");
        FFmpegBuilder ffmpegBuilder = builder.done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(ffmpegBuilder).run();

    }
}
