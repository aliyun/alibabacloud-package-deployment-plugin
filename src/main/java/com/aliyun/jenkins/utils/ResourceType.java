package com.aliyun.jenkins.utils;

/**
 *
 */
public enum ResourceType {

    ESS("ESS", "ESS ScalingGroup"),
    ECS("ECS", "ECS Instance");

    String type;
    String name;

    ResourceType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
