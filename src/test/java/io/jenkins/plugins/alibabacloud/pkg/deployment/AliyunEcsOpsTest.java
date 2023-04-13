package io.jenkins.plugins.alibabacloud.pkg.deployment;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class AliyunEcsOpsTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    public void testConfig() throws Exception {
        HtmlPage page = j.new WebClient().goTo("configure");
        WebAssert.assertTextPresent(page, "Alibaba Cloud");
    }
}
