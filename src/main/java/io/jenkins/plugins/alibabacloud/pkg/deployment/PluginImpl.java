package io.jenkins.plugins.alibabacloud.pkg.deployment;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * plugin extension：
 * global setting
 */
@Extension
@Symbol("pipelineStepsAliyun")
public class PluginImpl extends GlobalConfiguration {
    @SuppressWarnings("lgtm[jenkins/plaintext-storage]")
    private String accessKeyId;
    private Secret accessKeySecret;

    @DataBoundConstructor
    public PluginImpl() {
        load();
    }

    @DataBoundSetter
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    @DataBoundSetter
    public void setAccessKeySecret(Secret accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        accessKeyId = json.getString("accessKeyId");
        accessKeySecret = Secret.fromString(json.getString("accessKeySecret"));
        save();
        return true;
    }

    public static PluginImpl getInstance() {
        return ExtensionList.lookup(PluginImpl.class).get(0);
    }

    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    public String getAccessKeySecret() {
        return Secret.toString(this.accessKeySecret);
    }

}
