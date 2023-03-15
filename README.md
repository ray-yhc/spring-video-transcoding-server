# HLS 동영상 스트리밍 서비스
## 트랜스코딩 서버

![](https://velog.velcdn.com/images/raycho521/post/a699b887-5ad8-4d1b-a812-3a1d7839c114/image.png)


[미디어 공유 프로토콜 비교분석](https://separated-garden-7c6.notion.site/1-467116d1a32f450c88ec7fd77aee2351)


## 프로젝트 동기

- 동영상이 사용자에게 전달되는 과정을 구현해보자는 목표로 시작하였습니다.
- 스트리밍, 화상 통신 등에 사용하는 프로토콜을 조사한 뒤, 송출에는 RTMP 프로토콜, 스트리밍에는 HLS 프로토콜을 이용해 구현하기로 하였습니다.

## 세부기능

### 스트리밍 서버

[Github Link](https://github.com/ray-yhc/spring-video-streaming-server)

- 영상 파일 업로드
    - S3 presigned URL발급
    - 파일 세부정보 DB 저장
- 동영상 요청 및 HLS 스트리밍
- 트랜스코딩 서버에 인코딩 요청

### 트랜스코딩 서버

[Github Link](https://github.com/ray-yhc/spring-video-transcoding-server)

- 업로드된 영상 파일 인코딩 (ffmpeg)
- Adaptive bitrate streaming(ABS) 지원 - 240p, 480p, 1080p 화질 인코딩
- 인코딩 완료 후 파일 인코딩 상태 DB 저장