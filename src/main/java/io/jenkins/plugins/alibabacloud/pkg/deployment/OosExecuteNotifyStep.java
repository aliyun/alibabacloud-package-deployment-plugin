package io.jenkins.plugins.alibabacloud.pkg.deployment;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.oos.model.v20190601.CancelExecutionRequest;
import com.aliyuncs.oos.model.v20190601.ListExecutionsRequest;
import com.aliyuncs.oos.model.v20190601.ListExecutionsResponse;
import com.aliyuncs.oos.model.v20190601.NotifyExecutionRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.alibabacloud.pkg.deployment.utils.AliyunClientFactory;
import io.jenkins.plugins.alibabacloud.pkg.deployment.utils.Status;
import io.jenkins.plugins.alibabacloud.pkg.deployment.utils.StepUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.Set;

/**
 * This class is a  pipeline step:
 * oos template execution direction,like approve or cancle.
 */
public class OosExecuteNotifyStep extends Step {
    private final String region;
    //oos template execute id.
    private final String executeId;
    //oos template execution direction,like approve or cancle.
    private final String notifyType;

    public String getRegion() {
        return region;
    }

    public String getExecuteId() {
        return executeId;
    }

    public String getNotifyType() {
        return notifyType;
    }

    // bind data
    @DataBoundConstructor
    public OosExecuteNotifyStep(String region, String executeId, String notifyType) {
        this.region = region;
        this.executeId = executeId;
        this.notifyType = notifyType;
    }

    // step execution point.
    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext, this);
    }

    //plugin extension
    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return StepUtils.requiresDefault();
        }

        @Override
        public String getFunctionName() {
            return "oosExecuteNotify";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "notify oos Execute";
        }
    }

    //oos template execution direction,like approve or cancle.
    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = -6033716865337394259L;
        private final transient OosExecuteNotifyStep step;

        protected Execution(@NonNull StepContext context, OosExecuteNotifyStep step) {
            super(context);
            this.step = step;
        }

        /**
         * run include：
         * oos template execution direction.
         */
        @Override
        protected Void run() throws Exception {
            AliyunClientFactory instance = new AliyunClientFactory();
            instance.build(this.step.region);
            IAcsClient oosClient = instance.getOosClient();
            ListExecutionsRequest request = new ListExecutionsRequest();
            request.setExecutionId(this.step.executeId);
            String status = null;
            List<ListExecutionsResponse.Execution> executions = oosClient.getAcsResponse(request).getExecutions();
            if (executions.size() > 0) {
                status = executions.get(0).getStatus();
            } else {
                throw new Exception("oos executeId:" + this.step.executeId + "is not exist");
            }
            if ("Approve".equalsIgnoreCase(this.step.notifyType)) {
                if (Status.Waiting.name().equalsIgnoreCase(status)) {
                    NotifyExecutionRequest notifyExecutionRequest = new NotifyExecutionRequest();
                    notifyExecutionRequest.setExecutionId(this.step.executeId);
                    notifyExecutionRequest.setNotifyType(this.step.notifyType);
                    oosClient.getAcsResponse(notifyExecutionRequest);
                }
            }
            if ("Cancelled".equalsIgnoreCase(this.step.notifyType)) {
                if (!Status.Success.name().equalsIgnoreCase(status) && !Status.Failed.name().equalsIgnoreCase(status) && !Status.Cancelled.name().equalsIgnoreCase(status)) {
                    CancelExecutionRequest cancelExecutionRequest = new CancelExecutionRequest();
                    cancelExecutionRequest.setExecutionId(this.step.executeId);
                    oosClient.getAcsResponse(cancelExecutionRequest);
                }
            }
            return null;
        }
    }
}
