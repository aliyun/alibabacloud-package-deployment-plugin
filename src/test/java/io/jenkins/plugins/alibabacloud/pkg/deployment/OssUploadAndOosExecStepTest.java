package io.jenkins.plugins.alibabacloud.pkg.deployment;

import org.junit.Assert;
import org.junit.Test;

public class OssUploadAndOosExecStepTest {
    @Test
    public void gettersWorkAsExpected() {
        OssUploadAndOosExecStep step = new OssUploadAndOosExecStep("cn-hangzhou","ESS",
                "asg-XXXX","bucketTest","workspace/Test.zip","/","Automatic","EveryBatchPause",1,"/root/ceshi.zip",
                "");
        Assert.assertEquals("cn-hangzhou", step.getRegion());
        Assert.assertEquals("ESS", step.getResourceType());
        Assert.assertEquals("asg-XXXX", step.getResourceId());
        Assert.assertEquals("bucketTest", step.getBucket());
        Assert.assertEquals("workspace/Test.zip", step.getObjectName());
        Assert.assertEquals("/", step.getLocalPath());
        Assert.assertEquals("Automatic", step.getMode());
        Assert.assertEquals("EveryBatchPause", step.getPausePolicy());
        Assert.assertEquals(1, step.getBatchNumber());
        Assert.assertEquals("/root/ceshi.zip", step.getDestinationDir());
        Assert.assertEquals("", step.getInvokeScript());
    }
}
