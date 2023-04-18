Alibabacloud Package Deployment Jenkins plugin
=============================

The Alibabacloud Package Deployment Jenkins plugin provides a post-build step for your Jenkins
project. Upon a successful build, it will zip the workspace, upload to OSS, and
start a new deployment by OOS Automatic deployment. Optionally, you can set it to batch for the deployment to
finish, making the final success contingent on the success of the deployment.

Setting up
----------

After building and installing the plugin, some simple configuration is needed
for your project.

**Freestyle**

1. Open up your project configuration
1. In the `Post-build Actions` section, select "Alibabacloud Automatic Package Deployment"
1. ResourceType,ResourceId, objectName, bucket, and region are so on all
   required options.
1. Temporary access keys. These will use the global keys from the Jenkins
   instance.

**Pipeline**

1.  Create a [Jenkins Pipeline](https://plugins.jenkins.io/workflow-aggregator/) project
1.  Use the Pipeline Snippet Generator
1.  For 'Sample Step', choose 'step: General Build Step'
1.  For 'Build Step', choose 'Alibabacloud Automatic Package Deployment'
1.  populate variables and then 'Generate Groovy'

Here is a rather blank example:

	step([$class: 'AliyunEcsOpsByOssFilePublisher', region: 'cn-hangzhou', resourceType: 'ESS', resourceId: '', bucket: '', objectName: '', localPath: '', destinationDir: '', batchNumber: 1, invokeScript: ''])

