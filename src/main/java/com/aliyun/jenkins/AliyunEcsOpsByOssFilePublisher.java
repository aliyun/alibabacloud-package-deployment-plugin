package com.aliyun.jenkins;

import com.alibaba.fastjson.JSON;
import com.aliyun.jenkins.utils.ResourceType;
import com.aliyun.oss.model.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.oos.model.v20190601.ListExecutionsRequest;
import com.aliyuncs.oos.model.v20190601.StartExecutionRequest;
import com.aliyuncs.oos.model.v20190601.StartExecutionResponse;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.*;
import java.util.*;

/**
 * This class is a jenkins execute step:
 * apply to freestyle job post bulid or pipeline step
 */
public class AliyunEcsOpsByOssFilePublisher extends Publisher implements SimpleBuildStep {
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

    //OOS template download OSS bucket File to ECS specific file path
    private final String destinationDir;

    //OOS template number of execution batches.
    private final int batchNumber;

    //OOS template execute script on ECS after download OSS bucket File to ECS specific file path.
    private final String invokeScript;

    // bind data
    @DataBoundConstructor
    public AliyunEcsOpsByOssFilePublisher(String region, String resourceType, String resourceId, String bucket, String objectName, String localPath, String destinationDir, int batchNumber, String invokeScript) {
        this.region = region;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.bucket = bucket;
        this.objectName = objectName;
        this.localPath = localPath;
        this.destinationDir = destinationDir;
        this.batchNumber = batchNumber;
        this.invokeScript = invokeScript;
    }

    // getXXX functions are obtain data.
    public int getBatchNumber() {
        return batchNumber;
    }

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

    public String getDestinationDir() {
        return destinationDir;
    }

    public String getInvokeScript() {
        return invokeScript;
    }

    /**
     * perform is core function: it can be automatically executed based on the project built.
     */
    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        /**
         * get and check localPath
         * */
        final Map<String, String> envVars = build.getEnvironment(listener);
        String localPathFromEnv = getLocalPathFromEnv(envVars);
        String localPath = StringUtils.trimToEmpty(localPathFromEnv);
        if (!localPath.isEmpty() && !localPath.startsWith("/")) {
            localPath = "/" + localPath;
        }
        FilePath sourcePath = workspace.withSuffix(localPath).absolutize();
        if (!sourcePath.isDirectory() || !isSubDirectory(workspace, sourcePath)) {
            throw new IllegalArgumentException("Provided path (resolved as '" + sourcePath
                    + "') is not a subdirectory of the workspace (resolved as '" + workspace + "')");
        }
        final FilePath sourceDirectory = sourcePath;
        //acquired build name
        final String projectName = build.getDisplayName();
        //log util
        final PrintStream logger = listener.getLogger();
        listener.getLogger().println("resourceType:" + resourceType);
        final AliyunClients aliyun;
        // acquired ALIYUNClients: include:oos client & oss client.
        aliyun = new AliyunClients(region, getDescriptor().getAccessKeyId(), getDescriptor().getAccessKeySecret());
        // Compress and upload the specific path of the built project.
        zipAndUpload(aliyun, projectName, sourceDirectory, logger);
        //batch automatic execution oos template according to resource type.
        String executionId = null;
        StartExecutionRequest request = new StartExecutionRequest();
        //query execute status until execution end.
        ListExecutionsRequest executionsRequest = new ListExecutionsRequest();
        if ("ESS".equals(resourceType)) {
            // ess resource execute template.
            executionId = essResourceExec(request, aliyun, listener);
            executionsRequest.setExecutionId(executionId);
            executionsRequest.setTemplateName(ESS_TEMPLATE_NAME);
        } else {
            // ecs resource execute template.
            executionId = ecsResourceExec(request, aliyun, listener);
            executionsRequest.setExecutionId(executionId);
            executionsRequest.setTemplateName(ECS_TEMPLATE_NAME);
        }

        String status = null;
        while (!"Success".equalsIgnoreCase(status) && !"Failed".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status)) {
            try {
                Thread.sleep(500);
                status = aliyun.oosClient.getAcsResponse(executionsRequest).getExecutions().get(0).getStatus();
                listener.getLogger().println("ExecutionId Status:" + status);
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }
    }

    // ess resource  execute oos Template : ESS_TEMPLATE_NAME
    private String essResourceExec(StartExecutionRequest request, AliyunClients aliyun, TaskListener listener) {
        request.setTemplateName(ESS_TEMPLATE_NAME);
        String parameter =
                "{\"invokeDestinationDir\":\"" +
                        "" + destinationDir + "" +
                        "\"," +
                        "\"scalingGroupId\":\"" +
                        "" + resourceId + "" +
                        "\"," +
                        "\"invokeScript\":\"" +
                        invokeScript +
                        "\"," +
                        "\"OSSRegion\":\"" +
                        "" + region + "" +
                        "\"," +
                        "\"whetherSaveToFile\":true," +
                        "\"URLExpirationTime\":6000," +
                        "\"batchNumber\":" +
                        "" +
                        this.batchNumber +
                        "" +
                        "," +
                        "\"rollbackBucketName\":\"" +
                        "" + bucket + "" +
                        "\"," +
                        "\"rollbackObjectName\":\"" +
                        "" + objectName + "" +
                        "\"," +
                        "\"rollbackDestinationDir\":\"" +
                        "" + destinationDir + "" +
                        "\"," +
                        "\"invokeBucketName\":\"" +
                        "" + bucket + "" +
                        "\"," +
                        "\"invokeObjectName\":\"" +
                        "" + objectName + "" +
                        "\"," +
                        "\"invokeType\":\"" +
                        "" + "invoke" + "" +
                        "\"}";
        request.setParameters(parameter);
        StartExecutionResponse response = null;
        String executionId = null;
        try {
            response = aliyun.oosClient.getAcsResponse(request);
            executionId = response.getExecution().getExecutionId();
            listener.getLogger().println("you can login aliyun oos console to query oos template implementation progress:" + "https://oos.console.aliyun.com/" + region + "/execution/detail/" + executionId);
        } catch (ClientException e) {
            listener.getLogger().println("execute oos template error info:");
            listener.getLogger().println(e);
        }
        return executionId;
    }

    // ecs resource  execute oos Template : ECS_TEMPLATE_NAME
    private String ecsResourceExec(StartExecutionRequest request, AliyunClients aliyun, TaskListener listener) {
        request.setTemplateName(ECS_TEMPLATE_NAME);
        String[] instanceIdString = this.resourceId.split(",");
        HashSet<String> hashSet = new HashSet<>(Arrays.asList(instanceIdString));
        int ids = hashSet.size();
        int batchNumber = this.batchNumber;
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
                        this.bucket +
                        "\"," +
                        "\"objectName\":\"" +
                        this.objectName +
                        "\"," +
                        " \"rateControl\": " +
                        "{\"Mode\": \"Batch\"," +
                        " \"MaxErrors\": 0, " +
                        "\"BatchPauseOption\": \"" +
                        "Automatic" +
                        "\", " +
                        "\"Batch\": " +
                        batchSpeed +
                        "}, " +
                        "" +
                        "\"destinationDir\":\"" +
                        this.destinationDir +
                        "\"," +
                        "\"commandContent\":\"" +
                        this.invokeScript +
                        "\"," +
                        "\"targets\":" +
                        "{\"ResourceIds\":" +
                        instanceIds +
                        "," +
                        "\"RegionId\":\"" +
                        this.region +
                        "\"," +
                        "\"Type\":\"ResourceIds\"}," +
                        "\"OSSRegion\":\"" +
                        this.region +
                        "\"}";
        request.setParameters(parameter);
        StartExecutionResponse response = null;
        String executionId = null;
        try {
            response = aliyun.oosClient.getAcsResponse(request);
            executionId = response.getExecution().getExecutionId();
            listener.getLogger().println("you can login aliyun oos console:" + "https://oos.console.aliyun.com/" + this.region + "/execution" + " " + " to query OOS executionId :" + executionId);
        } catch (ClientException e) {
            listener.getLogger().println("execute oos template error info:");
            listener.getLogger().println(e);
        }
        return executionId;
    }

    //compress directory of localPath and upload to OSS.
    private void zipAndUpload(AliyunClients aliyun, String projectName, FilePath sourceDirectory, PrintStream logger) throws InterruptedException, IOException {
        File zipFile = zipProject(projectName, sourceDirectory, logger);
        uploadOssFile(aliyun, zipFile, logger);
    }

    private File zipProject(String projectName, FilePath sourceDirectory, PrintStream logger) throws IOException, InterruptedException {
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
        logger.println("Zipping files into " + zipFile.getAbsolutePath());

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
        return zipFile;
    }

    private void uploadOssFile(AliyunClients aliyun, File zipFile, PrintStream logger) {
        String bucketName = this.bucket;
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
            InitiateMultipartUploadResult upresult = aliyun.ossClient.initiateMultipartUpload(request);
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
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResult uploadPartResult = aliyun.ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = aliyun.ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            logger.println(completeMultipartUploadResult.getETag());
        } catch (Exception e) {
            logger.println(e);
        } finally {
            final boolean deleted = zipFile.delete();
            if (!deleted) {
                logger.println("Failed to clean up file " + zipFile.getPath());
            }
            if (aliyun.ossClient != null) {
                aliyun.ossClient.shutdown();
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
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
        return Util.replaceMacro(this.localPath, envVars);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * extension build step descriptor:
     * accessKeyId and accessKeySecret set global configuration in system management.
     * can set select component.
     */
    @Extension
    @Symbol("Alibabacloud Automatic Package Deployment")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String accessKeyId;
        private Secret accessKeySecret;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return Secret.toString(accessKeySecret);
        }

        public void setAccessKeySecret(Secret accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            accessKeyId = json.getString("accessKeyId");
            accessKeySecret = Secret.fromString(json.getString("accessKeySecret"));
            req.bindJSON(this, json);
            save();
            return super.configure(req, json);
        }

        public FormValidation doCheckObjectName(@QueryParameter String objectName) {
            if (objectName.startsWith("/"))
                return FormValidation.error("objectName can not start with '/'");
            return FormValidation.ok();
        }

        // select component about region.
        public ListBoxModel doFillRegionItems() throws ClientException {
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

        // select component about resource type.
        public ListBoxModel doFillResourceTypeItems() throws ClientException {
            ListBoxModel items = new ListBoxModel();
            for (ResourceType resourceType : ResourceType.values()) {
                items.add(resourceType.getName(), resourceType.getType());
            }
            return items;
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        //extension plugin name.
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Alibabacloud Automatic Package Deployment";
        }
    }

}
