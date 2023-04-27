package io.jenkins.plugins.alibabacloud.pkg.deployment.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import io.jenkins.plugins.alibabacloud.pkg.deployment.PluginImpl;

/**
 * product oos client and oss client.
 */
public class AliyunClientFactory {
    private static final String endpointFormat = "https://oss-%s.aliyuncs.com";

    private OSS ossClient;
    private IAcsClient oosClient;

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

    public void build(String region) {
        String accessKeyId = PluginImpl.getInstance().getAccessKeyId();
        String accessKeySecret = PluginImpl.getInstance().getAccessKeySecret();
        this.ossClient = new OSSClientBuilder().build(String.format(endpointFormat, region), accessKeyId, accessKeySecret);
        DefaultProfile profile = DefaultProfile.getProfile(
                region, accessKeyId, accessKeySecret
        );
        this.oosClient = new DefaultAcsClient(profile);
    }
}
