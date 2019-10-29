package org.dalnservice.classes;

import com.amazonaws.services.simpleemail.*;
import com.amazonaws.services.simpleemail.model.*;

/**
 * Created by Shakib on 5/25/2017.
 */
public class DALNSESClient {
    final String FROM = "daln@gsu.edu";

    private String BODY, SUBJECT;

    public void emailPostSubmitted(String TO, String postTitle) {
        BODY = "Thank you for submitting your post titled \"" + postTitle + "\" to the DALN! It is currently being"
                + " reviewed for approval.";
        SUBJECT = "DALN Post Submitted";

        sendEmail(TO);
    }

    public void emailAdminForApproval(String TO, String postTitle, String postId) {
        BODY = "A post titled \"" + postTitle + "\" has been submitted and is awaiting approval."
                + " You can view the post at the following link:\n" + "http://";
        SUBJECT = "DALN Post Awaiting Approval";

        sendEmail(TO);
    }

    public void emailPostApproved(String TO, String postTitle, String postId) {
        BODY = "Your post titled \"" + postTitle + "\" has been approved!"
                + " You can view your post at the following link:\n" + "http://";
        SUBJECT = "DALN Post Approved";

        sendEmail(TO);
    }

    public void emailPostRejected(String TO, String postTitle, String reason) {
        BODY = "Your post titled \"" + postTitle + "\" has been rejected" + " with the following explanation:\n"
                + reason;
        SUBJECT = "DALN Post Rejected";

        sendEmail(TO);
    }

    public void sendEmail(String TO) {
        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(TO);

        // Create the subject and body of the message.
        Content subject = new Content().withData(SUBJECT);
        Content textBody = new Content().withData(BODY);
        Body body = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination)
                .withMessage(message);

        try {
            System.out.println("Attempting to send an email through Amazon SES by using the AWS SDK for Java...");

            // Instantiate an Amazon SES client, which will make the service call. The
            // service call requires your AWS credentials.
            // Because we're not providing an argument when instantiating the client, the
            // SDK will attempt to find your AWS credentials
            // using the default credential provider chain. The first place the chain looks
            // for the credentials is in environment variables
            // AWS_ACCESS_KEY_ID and AWS_SECRET_KEY.
            // For more information, see
            // http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html

            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.defaultClient();

            // Send the email.
            client.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("The email was not sent.");
            System.out.println("Error message: " + ex.getMessage());
        }
    }
}
