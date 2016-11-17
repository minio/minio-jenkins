# Minio-Storage
This is a simple Jenkins plugin that lets you upload Jenkins artifacts to a Minio Server

### Preconditions

- A Jenkins instance. Installation instructions available [here](https://jenkins.io/doc/book/getting-started/installing/).

- A Minio server instance. Installation instructions available [here](https://docs.minio.io/docs/minio-quickstart-guide).

### Build from source

- To build the plugin, you'll need maven installed on your system. On Ubuntu you can do that using 

```
$ sudo apt-get install mvn
```

- Once maven is installed, download the source code from this repo. Then browse to the folder you downloaded the source via CLI and run the command

```
$ mvn clean install
```

- This should create the `.hpi` plugin file. 

### Installation

- Open Jenkins homepage. Navigate to Manage Jenkins >> Manage Plugins >> Advanced >> Upload Plugin. Select the `.hpi` file that you built in the previous step. Then click *Upload*. This will install the plugin.

- Now let us configure the plugin. Navigate to Manage Jenkins >> Configure System >> Minio upload configuration. Enter the Minio server URL, AccessKey and SecretKey (available during Minio server installation) and click *Save*.

- Next step is to configure project specific details. Open the Jenkins Job whose artifacts you want to upload to Minio. Click Configure. You'll find a new *Post-build Actions* called **Upload build artifacts to Minio server**. Select the Minio server actions and enter the relevant details.

- Now, all the artifacts as selected under the Source field will be uploaded to your Minio server.

