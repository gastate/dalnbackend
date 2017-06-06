package org.dalnservice.classes;
import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.simpleemail.*;
import com.amazonaws.services.simpleemail.model.*;
import com.amazonaws.regions.*;

/**
 * Created by Shakib on 5/25/2017.
 */
public class DALNSESClient
{
    public void sendEmail(String toEmail)
    {
        String FROM = "daln@gsu.edu";  // Replace with your "From" address. This address must be verified.
        String TO = toEmail; // Replace with a "To" address. If your account is still in the
        // sandbox, this address must be verified.
        String BODY = "This email was sent through Amazon SES by using the AWS SDK for Java.";
        String SUBJECT = "Amazon SES test (AWS SDK for Java)";

        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(TO);

        // Create the subject and body of the message.
        Content subject = new Content().withData(SUBJECT);
        Content textBody = new Content().withData(BODY);
        Body body = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination).withMessage(message);

        try
        {
            System.out.println("Attempting to send an email through Amazon SES by using the AWS SDK for Java...");

            // Instantiate an Amazon SES client, which will make the service call. The service call requires your AWS credentials.
            // Because we're not providing an argument when instantiating the client, the SDK will attempt to find your AWS credentials
            // using the default credential provider chain. The first place the chain looks for the credentials is in environment variables
            // AWS_ACCESS_KEY_ID and AWS_SECRET_KEY.
            // For more information, see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
            EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
            AWSCredentials awsCredentials = creds.getCredentials();
            AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient(awsCredentials);

            Region REGION = Region.getRegion(Regions.US_EAST_1);
            client.setRegion(REGION);

            // Send the email.
            client.sendEmail(request);
            System.out.println("Email sent!");
        }
        catch (Exception ex)
        {
            System.out.println("The email was not sent.");
            System.out.println("Error message: " + ex.getMessage());
        }
    }

}
