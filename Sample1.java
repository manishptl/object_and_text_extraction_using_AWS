package com.amazonaws.samples;

import java.io.IOException;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Sample1 {

    public static void main(String[] args) throws IOException {

    	String  sqsurl = "https://sqs.us-east-1.amazonaws.com/893482632515/myQueue.fifo";
    	String bucketName = "njit-cs-643";

    	
        AWSCredentials credentials = null;
        credentials = new ProfileCredentialsProvider("default").getCredentials();

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(new ProfileCredentialsProvider())
            .withRegion("us-east-1")
            .build();

        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(credentials))
              .withRegion(Regions.US_EAST_1)
              .build();

        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("us-east-1").build();

        
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);

        
        ListObjectsV2Result result;

        result = s3.listObjectsV2(req);

        int id=0;
        for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
              String key_photo = objectSummary.getKey().toString();
              
              DetectLabelsRequest request = new DetectLabelsRequest()
            	         .withImage(new Image()
            	         .withS3Object(new S3Object()
            	         .withName(key_photo).withBucket(bucketName)))
            	         .withMinConfidence(90F);
              System.out.println(key_photo+" "+id);
              DetectLabelsResult imageResult = rekognitionClient.detectLabels(request);
              List <Label> labels = imageResult.getLabels();

              for (Label label: labels) {

            	  if(label.getName().equals("Car")) {
            		  SendMessageRequest sr = new SendMessageRequest(sqsurl, key_photo).withMessageDeduplicationId(id++ +"");
            		  sr.setMessageGroupId("Msg1");
            		  sqs.sendMessage(sr);
            		  System.out.println("for + if "+key_photo);
            	  }
              }
              SendMessageRequest sr = new SendMessageRequest(sqsurl, "-1").withMessageDeduplicationId(id++ +"");
    		  sr.setMessageGroupId("Msg1");
    		  sqs.sendMessage(sr);
        }
        
    }
}
