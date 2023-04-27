package io.jenkins.plugins.alibabacloud.pkg.deployment;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.oos.model.v20190601.ListExecutionsRequest;
import com.aliyuncs.oos.model.v20190601.StartExecutionRequest;
import com.aliyuncs.oos.model.v20190601.StartExecutionResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.alibabacloud.pkg.deployment.utils.AliyunClientFactory;
import io.jenkins.plugins.alibabacloud.pkg.deployment.utils.StepUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.*;
import java.util.*;
/**
 * This class is a  pipeline step:
 * 1.compress specific directory
 * 2.upload to oss
 * 3.execute oos template (download oss file to ecs specific path)
 */
public class OssUploadAndOosExecStep extends Step {
    /**
     * ESS_TEMPLATE_NAME and  ECS_TEMPLATE_NAME  are OOS template name.
     */
    private final static String ESS_TEMPLATE_NAME = "ACS-ESS-RollingUpdateByDownloadOSSFileAndRunCommand";
    private final static String ECS_TEMPLATE_NAME = "ACS-ECS-BulkyDownloadOSSFileAndRunCommand";

    //ess or ecs resource location
    private final String region;

    //resource type: ESS or ECS
    private final String resourceType;

    /**
     * resource id：
     * ESS：scalingGroupId
     * ECS: instanceIds by comma separated
     */
    private final String resourceId;
    // oss bucket name.
    private final String bucket;
    // bulit project upload OSS bucket specific path.
    private final String objectName;
    //package project path：must be directory.
    private final String localPath;
    //the OOS template execution mode. Valid values：Automatic、FailurePause、Debug.
    private final String mode;
    //the OOS template loop mode. Valid values: Automatic、FirstBatchPause、EveryBatchPause.
    private final String pausePolicy;
    //OOS template number of execution batches.
    private final int batchNumber;
    //OOS template download OSS bucket File to ECS specific file path
    private final String destinationDir;
    //OOS template execute script on ECS after download OSS bucket File to ECS specific file path.
    private final String invokeScript;

    // getXXX functions are obtain data.
    public String getRegion() {
        return region;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getMode() {
        return mode;
    }

    public String getPausePolicy() {
        return pausePolicy;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public String getDestinationDir() {
        return destinationDir;
    }

    public String getInvokeScript() {
        return invokeScript;
    }

    // bind data
    @DataBoundConstructor
    public OssUploadAndOosExecStep(String region, String resourceType, String resourceId, String bucket, String objectName, String localPath, String mode, String pausePolicy, int batchNumber, String destinationDir, String invokeScript) {
        this.region = region;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.bucket = bucket;
        this.objectName = objectName;
        this.localPath = localPath;
        this.mode = mode;
        this.pausePolicy = pausePolicy;
        this.batchNumber = batchNumber;
        this.destinationDir = destinationDir;
        this.invokeScript = invokeScript;
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
            return StepUtils.requires(TaskListener.class, Run.class, FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "ossUploadAndOosExec";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "OSS upload built project and OOS execute";
        }

        public ListBoxModel doFillRegionItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("华东1（杭州）", "cn-hangzhou");
            model.add("华北1（青岛）", "cn-qingdao");
            model.add("华北2（北京）", "cn-beijing");
            model.add("华北3（张家口）", "cn-zhangjiakou");
            model.add("华北5（呼和浩特）", "cn-huhehaote");
            model.add("华北6（乌兰察布）", "cn-wulanchabu");
            model.add("华东2（上海）", "cn-shanghai");
            model.add("华南1（深圳）", "cn-shenzhen");
            model.add("华南2（河源）", "cn-heyuan");
            model.add("华南3（广州）", "cn-guangzhou");
            model.add("西南1（成都）", "cn-chengdu");
            model.add("中国（香港）", "cn-hongkong");
            model.add("亚太东北 1 (东京)", "ap-northeast-1");
            model.add("韩国（首尔）", "ap-northeast-2");
            model.add("亚太东南 1 (新加坡)", "ap-southeast-1");
            model.add("亚太东南 2 (悉尼)", "ap-southeast-2");
            model.add("亚太东南 3 (吉隆坡)", "ap-southeast-3");
            model.add("菲律宾（马尼拉）", "ap-southeast-6");
            model.add("亚太东南 5 (雅加达)", "ap-southeast-5");
            model.add("亚太南部 1 (孟买)", "ap-south-1");
            model.add("泰国（曼谷）", "ap-southeast-7");
            model.add("美国东部 1 (弗吉尼亚)", "us-east-1");
            model.add("美国西部 1 (硅谷)", "us-west-1");
            model.add("英国 (伦敦)", "eu-west-1");
            model.add("中东东部 1 (迪拜)", "me-east-1");
            model.add("沙特（利雅得)", "me-central-1");
            model.add("欧洲中部 1 (法兰克福)", "eu-central-1");
            return model;
        }

        public ListBoxModel doFillResourceTypeItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("ESS ScalingGroup", "ESS");
            model.add("ECS Instance", "ECS");
            return model;
        }

        public ListBoxModel doFillModeItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("自动执行", "Automatic");
            model.add("失败暂停", "FailurePause");
            model.add("单步执行", "Debug");
            return model;
        }

        @RequirePOST
        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        public FormValidation doCheckObjectName(@QueryParameter String objectName) {
            if (objectName.startsWith("/"))
                return FormValidation.error("objectName can not start with '/'");
            return FormValidation.ok();
        }

        public ListBoxModel doFillPausePolicyItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("不暂停", "Automatic");
            model.add("第一批暂停", "FirstBatchPause");
            model.add("每批暂停", "EveryBatchPause");
            return model;
        }

    }

    // specific behavior of execution and return oos template execute id.
    public static class Execution extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;
        private final transient OssUploadAndOosExecStep step;

        protected Execution(@NonNull StepContext context, OssUploadAndOosExecStep step) {
            super(context);
            this.step = step;
        }

        /**
         * run include:
         * 1.compress specific directory
         * 2.upload compressed file to oss
         * 3.OOS template execute script on ECS after OOS template download OSS bucket File to ECS specific file path.
         */
        @Override
        protected String run() throws Exception {
            AliyunClientFactory instance = new AliyunClientFactory();
            instance.build(this.step.region);
            IAcsClient oosClient = instance.getOosClient();
            OSS ossClient = instance.getOssClient();
            TaskListener listener = getContext().get(TaskListener.class);
            Run build = getContext().get(Run.class);
            FilePath workspace = getContext().get(FilePath.class);
            final Map<String, String> envVars = build.getEnvironment(listener);
            String localPathFromEnv = getLocalPathFromEnv(envVars);
            String localPath = StringUtils.trimToEmpty(localPathFromEnv);
            if (!this.step.localPath.isEmpty() && !this.step.localPath.startsWith("/")) {
                localPath = "/" + this.step.localPath;
            }
            FilePath sourcePath = workspace.withSuffix(localPath).absolutize();
            if (!sourcePath.isDirectory() || !isSubDirectory(workspace, sourcePath)) {
                throw new IllegalArgumentException("Provided path (resolved as '" + sourcePath
                        + "') is not a subdirectory of the workspace (resolved as '" + workspace + "')");
            }
            final FilePath sourceDirectory = sourcePath;
            final String projectName = build.getDisplayName();
            final PrintStream logger = listener.getLogger();
            zipAndUpload(ossClient, projectName, sourceDirectory, logger);
            String executionId = null;
            StartExecutionRequest request = new StartExecutionRequest();
            request.setMode(this.step.mode);
            ListExecutionsRequest executionsRequest = new ListExecutionsRequest();
            if ("ESS".equals(this.step.resourceType)) {
                executionId = essResourceExec(request, oosClient, listener);
                executionsRequest.setExecutionId(executionId);
                executionsRequest.setTemplateName(ESS_TEMPLATE_NAME);
            } else {
                executionId = ecsResourceExec(request, oosClient, listener);
                executionsRequest.setExecutionId(executionId);
                executionsRequest.setTemplateName(ECS_TEMPLATE_NAME);
            }
            String status = null;
            while (!"Success".equalsIgnoreCase(status) && !"Failed".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status) && !"Waiting".equalsIgnoreCase(status)) {
                try {
                    Thread.sleep(5000);
                    status = oosClient.getAcsResponse(executionsRequest).getExecutions().get(0).getStatus();
                    listener.getLogger().println("ExecutionId Status:" + status);
                } catch (ClientException e) {
                    e.printStackTrace();
                }
            }
            return executionId;
        }

        // ecs resource  execute oos Template : ECS_TEMPLATE_NAME
        private String ecsResourceExec(StartExecutionRequest request, IAcsClient oosClient, TaskListener listener) {
            request.setTemplateName(ECS_TEMPLATE_NAME);
            String[] instanceIdString = this.step.resourceId.split(",");
            HashSet<String> hashSet = new HashSet<>(Arrays.asList(instanceIdString));
            int ids = hashSet.size();
            int batchNumber = this.step.batchNumber;
            if(ids < batchNumber){
                batchNumber = ids;
            }
            int base = ids / batchNumber;
            int[] batchArray = new int[batchNumber];
            Arrays.fill(batchArray, base);
            for (int i = ids % batchNumber; i > 0; i--) {
                batchArray[i - 1]++;
            }
            String batchSpeed = JSON.toJSON(batchArray).toString();
            List<String> list = new ArrayList<>(hashSet);
            String instanceIds = JSON.toJSON(list).toString();
            String parameter =
                    "{\"bucketName\":\"" +
                            this.step.bucket +
                            "\"," +
                            "\"objectName\":\"" +
                            this.step.objectName +
                            "\"," +
                            " \"rateControl\": " +
                            "{\"Mode\": \"Batch\"," +
                            " \"MaxErrors\": 0, " +
                            "\"BatchPauseOption\": \"" +
                            this.step.pausePolicy +
                            "\", " +
                            "\"Batch\": " +
                            batchSpeed +
                            "}, " +
                            "" +
                            "\"destinationDir\":\"" +
                            this.step.destinationDir +
                            "\"," +
                            "\"commandContent\":\"" +
                            this.step.invokeScript +
                            "\"," +
                            "\"targets\":" +
                            "{\"ResourceIds\":" +
                            instanceIds +
                            "," +
                            "\"RegionId\":\"" +
                            this.step.region +
                            "\"," +
                            "\"Type\":\"ResourceIds\"}," +
                            "\"OSSRegion\":\"" +
                            this.step.region +
                            "\"}";
            request.setParameters(parameter);
            StartExecutionResponse response = null;
            String executionId = null;
            try {
                response = oosClient.getAcsResponse(request);
                executionId = response.getExecution().getExecutionId();
                listener.getLogger().println("you can login aliyun oos console to query oos template implementation progress:" + "https://oos.console.aliyun.com/" + this.step.region + "/execution/detail/" + executionId);
            } catch (ClientException e) {
                listener.getLogger().println("execute oos template error info:");
                listener.getLogger().println(e);
            }
            return executionId;
        }

        // ess resource  execute oos Template : ESS_TEMPLATE_NAME
        private String essResourceExec(StartExecutionRequest request, IAcsClient oosClient, TaskListener listener) {
            request.setTemplateName(ESS_TEMPLATE_NAME);
            String parameter =
                    "{\"invokeDestinationDir\":\"" +
                            "" + this.step.destinationDir + "" +
                            "\"," +
                            "\"scalingGroupId\":\"" +
                            "" + this.step.resourceId + "" +
                            "\"," +
                            "\"invokeScript\":\"" +
                            this.step.invokeScript +
                            "\"," +
                            "\"OSSRegion\":\"" +
                            "" + this.step.region + "" +
                            "\"," +
                            "\"batchPauseOption\":\"" +
                            "" + this.step.pausePolicy + "" +
                            "\"," +
                            "\"whetherSaveToFile\":true," +
                            "\"URLExpirationTime\":" +
                            "6000" +
                            "," +
                            "\"batchNumber\":" +
                            "" +
                            this.step.batchNumber +
                            "" +
                            "," +
                            "\"rollbackBucketName\":\"" +
                            "" + this.step.bucket + "" +
                            "\"," +
                            "\"rollbackObjectName\":\"" +
                            "" + this.step.objectName + "" +
                            "\"," +
                            "\"rollbackDestinationDir\":\"" +
                            "" + this.step.destinationDir + "" +
                            "\"," +
                            "\"invokeBucketName\":\"" +
                            "" + this.step.bucket + "" +
                            "\"," +
                            "\"invokeObjectName\":\"" +
                            "" + this.step.objectName + "" +
                            "\"," +
                            "\"invokeType\":\"" +
                            "" + "invoke" + "" +
                            "\"}";
            request.setParameters(parameter);
            StartExecutionResponse response = null;
            String executionId = null;
            try {
                response = oosClient.getAcsResponse(request);
                executionId = response.getExecution().getExecutionId();
                listener.getLogger().println("you can login aliyun oos console to query oos template implementation progress:" + "https://oos.console.aliyun.com/" + this.step.region + "/execution/detail/" + executionId);
            } catch (ClientException e) {
                listener.getLogger().println("execute oos template error info:");
                listener.getLogger().println(e);
            }
            return executionId;
        }

        //compress directory of localPath and upload to OSS.
        private void zipAndUpload(OSS ossClient, String projectName, FilePath sourceDirectory, PrintStream logger) throws InterruptedException, IOException {
            File zipFile = null;
            File versionFile;
            versionFile = new File(sourceDirectory + "/");
            InputStreamReader reader = null;
            String version = null;
            try {
                reader = new InputStreamReader(new FileInputStream(versionFile), "UTF-8");
                char[] chars = new char[(int) versionFile.length() - 1];
                reader.read(chars);
                version = new String(chars);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            if (version != null) {
                zipFile = new File("/tmp/" + projectName + "-" + version + ".zip");
                final boolean fileCreated = zipFile.createNewFile();
                if (!fileCreated) {
                    logger.println("File already exists, overwriting: " + zipFile.getPath());
                }
            } else {
                zipFile = File.createTempFile(projectName + "-", ".zip");
            }

            FileOutputStream outputStream = new FileOutputStream(zipFile);
            try {
                sourceDirectory.zip(
                        outputStream,
                        new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return true;
                            }
                        }
                );
            } finally {
                outputStream.close();
            }
            String bucketName = this.step.bucket;

            try {
                InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, this.step.objectName);
                InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(request);
                String uploadId = upresult.getUploadId();
                List<PartETag> partETags = new ArrayList<PartETag>();
                final long partSize = 1 * 1024 * 1024L;
                long fileLength = zipFile.length();
                int partCount = (int) (fileLength / partSize);
                if (fileLength % partSize != 0) {
                    partCount++;
                }
                for (int i = 0; i < partCount; i++) {
                    long startPos = i * partSize;
                    long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                    InputStream instream = new FileInputStream(zipFile);
                    instream.skip(startPos);
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(this.step.objectName);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setInputStream(instream);
                    uploadPartRequest.setPartSize(curPartSize);
                    uploadPartRequest.setPartNumber(i + 1);
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    partETags.add(uploadPartResult.getPartETag());
                }
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, this.step.objectName, uploadId, partETags);
                CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                logger.println(completeMultipartUploadResult.getETag());
            } catch (Exception e) {
                logger.println(e);
            } finally {
                final boolean deleted = zipFile.delete();
                if (!deleted) {
                    logger.println("Failed to clean up file " + zipFile.getPath());
                }
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        }

        private boolean isSubDirectory(FilePath parent, FilePath child) {
            FilePath parentFolder = child;
            while (parentFolder != null) {
                if (parent.equals(parentFolder)) {
                    return true;
                }
                parentFolder = parentFolder.getParent();
            }
            return false;
        }

        public String getLocalPathFromEnv(Map<String, String> envVars) {
            return Util.replaceMacro(this.step.localPath, envVars);
        }
    }

}
