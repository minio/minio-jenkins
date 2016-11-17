package org.jenkinsci.plugins.minio;

import hudson.Launcher;
import hudson.Util;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.xmlpull.v1.XmlPullParserException;

import javax.annotation.Nonnull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InputSizeMismatchException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link MinioUploader} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Nitish Tiwari
 *
 */
public final class MinioUploader extends Recorder implements SimpleBuildStep {

	/**
     * File name relative to the workspace root to upload.
     * Can contain macros and wildcards.
     */
    public String sourceFile;
    
    /**
     * File name relative to the workspace root to be excluded from upload.
     * Can contain macros and wildcards.
     */
    public String excludedFile;
    
    /**
     * File name relative to the workspace root to be excluded from upload.
     * Can contain macros and wildcards.
     */
    public String bucketName;
    
    /**
     * Prefix to be added to the object name while.
     */
    public String objectNamePrefix;
        
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public MinioUploader(String sourceFile, String excludedFile, String bucketName, String objectNamePrefix) {
    	this.sourceFile = sourceFile;
        this.excludedFile = excludedFile;
        this.bucketName = bucketName;
        this.objectNamePrefix = objectNamePrefix;
    }
    
    private void log(final PrintStream logger, final String message) {
        logger.println(StringUtils.defaultString(getDescriptor().getDisplayName()) + ' ' + message);
    }
    
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
    	final PrintStream console = listener.getLogger();
        
    	if (Result.ABORTED.equals(run.getResult())) {
			log(console, "Skipping publishing on Minio because build aborted");
		    return;
		}

		try {
		        final Map<String, String> envVars = run.getEnvironment(listener);
		        boolean bucketFound = false;
		        
		        MinioClient minioClient = getDescriptor().getMinioClient();
		        
		        if (Result.FAILURE.equals(run.getResult())) {
	                // build failed. don't post
		            log(console, "Skipping publishing on Minio because build failed");
		        }
		        
		        final String expanded = Util.replaceMacro(sourceFile, envVars);
		        final String exclude = Util.replaceMacro(excludedFile, envVars);
		        
		        if (expanded == null) {
		            throw new IOException();
		        }
		        
		        bucketFound = minioClient.bucketExists(bucketName);
		        // If bucket not present, create bucket
		        if(!bucketFound){
		          minioClient.makeBucket(bucketName);
		        }
		        
    	        for (String startPath : expanded.split(",")) {
		            for (FilePath path : ws.list(startPath, exclude)) {
		                if (path.isDirectory()) {
		                    throw new IOException(path + " is a directory");
		                }
		                
		                final int workspacePath = FileHelper.getSearchPathLength(ws.getRemote(),
                                startPath.trim());
                        
		                String fileName = getFilename(path, workspacePath);
                        long size = path.length();
                        String objectName; 
                        
                        InputStream stream = new FileInputStream(path.getRemote());
                        String contentType = "application/octet-stream";
                        
                        if(objectNamePrefix != null){
                        	objectName = objectNamePrefix + "_" + fileName;
                        } else {
                        	objectName = fileName;
                        }
                        
                        minioClient.putObject(bucketName, objectName, stream, size, contentType);
                        log(console, "bucket=" + bucketName + ", file=" + fileName + ", is uploaded");
						
		            }
    	        }
		
		} catch (InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException
				| InsufficientDataException | NoResponseException | ErrorResponseException
				| InternalException | InvalidArgumentException | InputSizeMismatchException
				| XmlPullParserException e) {
			e.printStackTrace(listener.error("Minio error"));
		    run.setResult(Result.UNSTABLE);
		} 
		catch (IOException e) {
		    e.printStackTrace(listener.error("Failed to upload files"));
		    run.setResult(Result.UNSTABLE);
		} catch (InterruptedException e) {
			e.printStackTrace(listener.error("Failed to upload files"));
		    run.setResult(Result.UNSTABLE);
		} 
    }

    private String getFilename(FilePath src, int searchIndex) {
        final String fileName;
        final String relativeFileName = src.getRemote();
            fileName = relativeFileName.substring(searchIndex);
        return fileName;
    }
    
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link MinioUploader}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String serverURL;
        private String accessKey;
        private String secretKey;
        private MinioClient minioClient;
        
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Upload build artifacts to Minio server";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
        	serverURL = formData.getString("serverURL");
            accessKey = formData.getString("accessKey");
            secretKey = formData.getString("secretKey");
            
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns server URL from global configuration.
         * 
         * @return
         * Returns the Minio Server URL
         */
        public String getServerURL() {
            return serverURL;
        }
        
        /**
         * This method returns Access Key from global configuration.
         * 
         * @return
         * Returns the Access Key for Minio Server
         */
        public String getAccessKey() {
            return accessKey;
        }
        
        /**
         * This method returns Secret Key from global configuration.
         * 
         * @return
         * Returns the Secret Key for Minio Server
         */
        public String getSecretKey() {
            return secretKey;
        }
        
        /**
         * This method returns Secret Key from global configuration.
         * 
         * @return
         * Returns the MinioClient object
         */
        public MinioClient getMinioClient() {
        	if(minioClient == null){
        		try {
					minioClient = new MinioClient(serverURL, accessKey, secretKey);
				} catch (InvalidEndpointException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidPortException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	return minioClient;
        }
    }

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}

}

