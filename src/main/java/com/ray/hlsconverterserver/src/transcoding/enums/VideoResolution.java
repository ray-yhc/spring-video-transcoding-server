package com.ray.hlsconverterserver.src.transcoding.enums;

public enum VideoResolution {
    P360("360p", "640x360"),
    P480("480p","800x480"),
    P720("720p","1280x720"),
    P1080("1080p","1920x1080")
    ;

    private final String label;
    private final String resolution;

    VideoResolution(String label, String resolution) {
        this.label = label;
        this.resolution = resolution;
    }
    public String resolution() {return resolution;};
    public String label() {return label;};
}
