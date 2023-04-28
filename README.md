Alibabacloud Package Deployment Jenkins plugin
=============================

Plugins for deployment package on Alibaba Cloud.
* The plugin provides a post-build step:upon a successful build,it will compress the specified working directory, upload it to OSS, and then download OSS files to ECS through OOS for deployment
* This plugins adds Jenkins pipeline steps to interact with the Aliyun API.


Setting up
----------

After building and installing the plugin, some simple configuration is needed
for your project.Please set global configuration of Aliyun Ak/Sk.

# How to use post-build step

### Freestyle

1. Open up your project configuration
2. In the `Post-build Actions` section, select "Alibabacloud Automatic Package Deployment"
3. ResourceType,ResourceId, objectName, bucket, and region are so on all
   required options.
4. Temporary access keys. These will use the global keys from the Jenkins
   instance.

### Pipeline

1.  Create a [Jenkins Pipeline](https://plugins.jenkins.io/workflow-aggregator/) project
2.  Use the Pipeline Snippet Generator
3.  For 'Sample Step', choose 'step: General Build Step'
4.  For 'Build Step', choose 'Alibabacloud Automatic Package Deployment'
5.  populate variables and then 'Generate Groovy'

Here is a rather blank example:

	step([$class: 'AliyunEcsOpsByOssFilePublisher', region: 'cn-hangzhou', resourceType: 'ESS', resourceId: '', bucket: '', objectName: '', localPath: '', destinationDir: '', batchNumber: 1, invokeScript: ''])

# How to use pipeline steps to interact with the Aliyun API

* [ossUploadAndOosExec](####ossUploadAndOosExec)
* [oosStatusQuery](####oosStatusQuery)
* [oosExecuteNotify](####oosExecuteNotify)

#### ossUploadAndOosExec

Upload built project to OSS and execution OOS template download OSS file to smartly deploy on ECS instances.

```groovy
executeId = ossUploadAndOosExec(batchNumber: 3, mode: 'FailurePause', bucket: 'testBucket', destinationDir: '/root/test.zip', invokeScript: '', localPath: '/', objectName: 'test.zip', pausePolicy: 'EveryBatchPause', region: 'cn-hangzhou', resourceId: 'asg-bp15XXXXX', resourceType: 'ESS')
```

##### oosStatusQuery

Query OOS template task status by OSS template task id.

```groovy
oosStatusQuery(executeId: "exec-XXXXXXXXX", region: 'cn-hangzhou')
```

#### oosExecuteNotify

Oos template pause task execution next step,like Approve or Cancelled.

```groovy
oosExecuteNotify(executeId: "exec-XXXXXXXXX", region: 'cn-hangzhou', notifyType: "Approve")
```