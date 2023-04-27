package io.jenkins.plugins.alibabacloud.pkg.deployment;

import org.junit.Assert;
import org.junit.Test;

public class OosStatusQueryStepTest {
    @Test
    public void gettersWorkAsExpected() {
        OosStatusQueryStep step = new OosStatusQueryStep("cn-hangzhou","exec-xxxxxx");
        Assert.assertEquals("cn-hangzhou", step.getRegion());
        Assert.assertEquals("exec-xxxxxx", step.getExecuteId());
    }
}
