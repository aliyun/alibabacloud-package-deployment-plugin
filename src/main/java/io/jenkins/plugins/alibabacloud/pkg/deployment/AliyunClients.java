package io.jenkins.plugins.alibabacloud.pkg.deployment;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;

/**
 * OOS and OSS client
 */
public class AliyunClients {
    private static final String endpointFormat = "https://oss-%s.aliyuncs.com";

    public  OSS ossClient;
    public  IAcsClient oosClient;

    public OSS getOssClient() {
        return ossClient;
    }

    public void setOssClient(OSS ossClient) {
        this.ossClient = ossClient;
    }

    public IAcsClient getOosClient() {
        return oosClient;
    }

    public void setOosClient(IAcsClient oosClient) {
        this.oosClient = oosClient;
    }

    public AliyunClients(String regionId, String accessKeyId, String accessKeySecret) {
        this.ossClient = new OSSClientBuilder().build(String.format(endpointFormat, regionId), accessKeyId, accessKeySecret);
        DefaultProfile profile = DefaultProfile.getProfile(
                regionId, accessKeyId, accessKeySecret
        );
        this.oosClient = new DefaultAcsClient(profile);
    }
}
