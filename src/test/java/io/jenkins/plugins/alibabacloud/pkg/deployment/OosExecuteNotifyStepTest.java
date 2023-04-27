package io.jenkins.plugins.alibabacloud.pkg.deployment;

import org.junit.Assert;
import org.junit.Test;

public class OosExecuteNotifyStepTest {
    @Test
    public void gettersWorkAsExpected(){
        OosExecuteNotifyStep step = new OosExecuteNotifyStep("cn-hangzhou","exec-xxxxxx","Approve");
        Assert.assertEquals("cn-hangzhou", step.getRegion());
        Assert.assertEquals("exec-xxxxxx", step.getExecuteId());
        Assert.assertEquals("Approve", step.getNotifyType());
    }
}
