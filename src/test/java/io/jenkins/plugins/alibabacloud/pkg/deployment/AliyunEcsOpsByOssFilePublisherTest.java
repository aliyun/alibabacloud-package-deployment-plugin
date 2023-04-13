package io.jenkins.plugins.alibabacloud.pkg.deployment;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class AliyunEcsOpsByOssFilePublisherTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testRoundTripConfiguration() throws Exception {
        final AliyunEcsOpsByOssFilePublisher publisher = new AliyunEcsOpsByOssFilePublisher(
                "cn-hangzhou",
                "ESS",
                "asg-XXXXXXXX",
                "testBucket",
                "/testLocation",
                "/",
                "/root",
                1,
                "");
       final  AliyunEcsOpsByOssFilePublisher afterPublisher = j.configRoundtrip(publisher);
        j.assertEqualDataBoundBeans(publisher, afterPublisher);
    }
    @Test
    @LocalData
    public void testSaveUsesSecret() throws Exception {
        FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("testSecrets");
        FreeStyleProject after = j.configRoundtrip(project);
        assertThat(after.getConfigFile().asString(), not(containsString("TEST_SECRET")));
    }
}
