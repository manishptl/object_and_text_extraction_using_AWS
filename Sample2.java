package com.amazonaws.samples;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class Sample2
{
    	public static void storeOP(List<String> data){
		try {
      			FileWriter myWriter = new FileWriter("filename.txt");
      			String toStore = "";
			for(String d:data)
				toStore = (toStore + d + "\n");
			myWriter.write(toStore);
      			myWriter.close();
    		} catch (IOException e) {
      			System.out.println("ERROR IN STORING THE FILE");
      			e.printStackTrace();
    		}
	}

public static String detectText(String photo, String bucket){
	    String ph = photo;
	    String out = "";
	    AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("us-east-1").build();
		    DetectTextRequest request = new DetectTextRequest()
			      .withImage(new Image()
			      .withS3Object(new S3Object()
			      .withName(photo)
			      .withBucket(bucket)));

              try {
                 DetectTextResult result = rekognitionClient.detectText(request);
                 List<TextDetection> textDetections = result.getTextDetections();	

                 for (TextDetection text: textDetections) {
                  out = ph+" "+text.getDetectedText();
                  break;
                 }
                } catch(AmazonRekognitionException e) {
                 e.printStackTrace();
              }	    
	      return out;
    }


public static void main( String[] args )
    {
	      String bucketName = "njit-cs-643";
	      String myQueueUrl = "https://sqs.us-east-1.amazonaws.com/893482632515/myQueue.fifo";
	      
	      AWSCredentials credentials = null;
	      credentials = new ProfileCredentialsProvider("default").getCredentials();
	        
	      AmazonSQS sqs = AmazonSQSClientBuilder.standard()
	              .withCredentials(new AWSStaticCredentialsProvider(credentials))
	              .withRegion(Regions.US_EAST_1)
	              .build();
	      
	      
	        ReceiveMessageRequest receiveMessageRequest =
                    new ReceiveMessageRequest(myQueueUrl).withMaxNumberOfMessages(10);
		
		List<String> output = new ArrayList<String>();
		boolean flag = true;
		while(flag){
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest)
				    .getMessages();
			
			for (Message message : messages) {
				if(message.getBody().matches("-1")){
					sqs.deleteMessage(myQueueUrl, message.getReceiptHandle());
					flag = false;
					break;
				}
			
					String t = detectText(message.getBody(), bucketName);
					sqs.deleteMessage(myQueueUrl, message.getReceiptHandle());
					output.add(t);
			}	
		}
		storeOP(output);
    }
}
