# Minio-Jenkins-Plugin
This is a simple Jenkins plugin that lets you upload Jenkins artifacts to a Minio Server

### Preconditions 

- A Jenkins instance. Installation instructions available [here](https://jenkins.io/).

- A Minio server instance. Installation instructions available [here](https://docs.minio.io/docs/minio-quickstart-guide).

### Installation 

- Download the latest plugin release from [here](https://github.com/NitishT/Minio-Jenkins-Plugin/releases).

- Open Jenkins homepage. Navigate to Manage Jenkins >> Manage Plugins >> Advanced >> Upload Plugin. Select the .hpi file that you downloaded in the previous step. Then click *Upload*. This will install the plugin. 

- Now let us configure the plugin. Navigate to Manage Jenkins >> Configure System >> Minio upload configuration. Enter the Minio server URL, AccessKey and SecretKey (available during Minio server installation) and click *Save*. 

- Next step is to configure project specific details. Open the Jenkins Job whose artifacts you want to upload to Minio. Click Configure. You'll find a new *Post-build Actions* called **Upload build artifacts to Minio server**. Select the Minio server actions and enter the relevant details. 

- Now, all the artifacts as selected under the Source field will be uploaded to your Minio server. 

### Acknowledgement

- Authors of [Jenkins S3 plugin](https://github.com/jenkinsci/s3-plugin) for providing a great place to get started. 
