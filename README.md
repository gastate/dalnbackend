# DALN REST API

This is the REST API for the DALN. It is handled through AWS Lambda and API Gateway.

## Setup and Deployment

This Java REST API has been migrated to a serverless architecture with the use of the Lambada Framework and AWS.
Lambada Framework is an open source project that implements common JAX-RS annotations 
and provides a Maven plugin to build and deploy the API to AWS.
It looks for the JAX-RS annotations at build time then creates Lambda
functions and API Gateway definitions.

More information about the Lambada Framework can be found [here](https://aws.amazon.com/blogs/compute/migrating-a-native-java-rest-api-to-a-serverless-architecture-with-the-lambada-framework-for-aws/) 
and [here](https://github.com/cagataygurturk/lambadaframework).

### Configuration

The project is built using Maven, so configuration is done in the pom files
(pom-dev.xml and pom-prod.xml, for development and production instances respectfully).


Options that are unique to each pom:

- `<artifactId>`: The name for our Lambda function and API. This property
allows us to create separate instances for both.
- `<deployment.bucket>`: The S3 bucket where the compiled JAR will be uploaded.
- `<deployment.stage>`: A stage is a named reference to a deployment, which is a snapshot of the API.
This is appended to the end of the final API endpoint. 

Other configuration notes:
- The original pom (pom.xml) is not used in the deployment process and should not be used.
- All dependencies must be added/updated on both dev and prod pom files.
- There is one jar file, `soundcloud-0.2.1-jar-with-dependencies.jar`, that is not
included in the pom dependencies. This jar, as well as future jars, must 
be installed to your local Maven repository.
- In the `maven-compiler-plugin`, the `<source>` and `<target>` properties
 must match the Project SDK (1.8 at the time of writing).

Once the configuration is set, run
`mvn deploy -f <path-to-pom-file>`
to build and deploy the API. A link to the API will be displayed at the end of 
 successful deployment. The Lambda functions and the APIs can
all be managed through the AWS Console.


### Authentication

The default AWS profile installed on the system must have administator privileges,
or at least the following IAM policy:

`
  {"Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Action": [
                        "cloudformation:*",
                        "s3:*",
                        "lambda:*",
                        "execute-api:*",
                        "apigateway:*",
                        "iam:*",
                        "ec2:DescribeSecurityGroups",
                        "ec2:DescribeVpcs",
                        "ec2:DescribeSubnets"
                    ],
                    "Resource": [
                        "*"
                    ]
                }
            ]
        }
`

To connect with any services in AWS, you must provide AWS credentials.
You can provide credentials by either creating an AWS credentials profile file
or setting environment variables with the access key and secret key. Detailed
instructions can be found [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html).

Other credentials or configurations that are used in the application (i.e. `System.getenv()`)
are defined as environment variables in the AWS console in the Lambda function.

### Application Overview

This section will detail generally how the [DALN](daln.gsu.edu) works and what components are involved.

On the DALN:
- Users can submit literacy narratives (the words "narrative" and "post" will be used interchangeably). A narrative consists of
(1) metadata about the narrative, such as a title, description, tags, etc. and
(2) files to be associated with their narrative, which can be of any type, but mainly
videos, audio files, and text documents.
- Users can search for and view existing narratives.
- Admins can approve and unapprove posts (user-submitted posts are initially unapproved
and must be approved before they can be searchable on the DALN).

Services used:
- Amazon S3: Each narrative has a folder in the S3 bucket. In this folder, there will
be the files associated with this post. A post cannot exist without at least one file associated with it.
- DynamoDB: The database in which posts are stored.
- SimpleQueueService: When a file needs to be uploaded, a message is generated and added
to the file upload queue. The service that handles file uploads is running on a separate
worker on an EC2 service. Information and details about this worker can be found [here](https://github.com/gastate/daln_upload_worker).
- CloudSearch: The search engine that contains all approved posts. Unapproving a post
removes the post from the search engine but the post will still exist in DynamoDB.
Note: This is the only service used where there is only **one** instance (search engine) for both
development and production. All other services have separate instances for development and production.

To create a post (using the REST functions):
1. `/posts/create`: Create a post with at least the required metadata fields. A record in DynamoDB will be created for this post.
2. `/asset/s3uploader`: Send a POST request to retrieve a pre-signed URL for the file you want to upload
with this post. Afterwards, place a PUT request to that URL so that the file will be uploaded
to the DALN staging area bucket. All files will initially be uploaded to this bucket.
3. `/asset/apiupload`: Send a POST request with a message to enter a specific file (to be uploaded)
to the SQS queue. The independent DALN upload worker will then transfer this file
to the appropriate CDN. Videos are uploaded to SproutVideo, audios are uploaded to SoundCloud, and
all other files remain in S3 but will be moved from the staging area to the specific post folder.
Afterwards, the DynamoDB record will be updated with the file location.

## REST Functions

### GET

| **Title**      | Get a Single Post (with Path Parameter)        |
| :---------:    | ------  |
| **URL**        | /posts/get/:postId |
| **URL Params** | Required: <br> id=[string] |
| **Description**| Supply the url with the post ID and have the individual post returned. |
| **Example**    | /posts/get/3b983676-d935-4a2c-b537-4becd5a545b5 |


Title: Get a Single Post (with Form Parameter)


| **Title**      | Get All Posts        |
| :---------:    | ------  |
| **URL**        | /posts/all |
| **URL Params** | None |
| **Description**| Have all posts in the database returned. |

| **Title**      | Search        |
| :---------:    | ------  |
| **URL**        | /posts/search/:query |
| **URL Params** | _Required:_ <br> query=[string] <br>
| **Description**| Search the database of posts and return the results. If only the query is specified, the first 10 results will be returned. |
| **Example**   | /posts/search/literacy

| **Title**      | Search (with Pagination)       |
| :---------:    | ------  |
| **URL**        | /posts/search/:query/:pageSize/:start |
| **URL Params** | _Required:_ <br> query=[string] <br> pageSize=[integer] <br> start=[integer] 
| **Description**| Search the database of posts and return the results with the page and page size specified. <br> "pageSize": the amount of results you want returned in each call. <br> "start": the index of the first post returned. |
| **Example**   | /posts/search/literacy/10/0 |

| **Title**      | Search (with Pagination and Options)       |
| :---------:    | ------  |
| **URL**        | /posts/search/:query/:pageSize/:start/:field/:order |
| **URL Params** | _Required:_ <br> query=[string] <br> pageSize=[integer] <br> start=[integer] <br> field=[string] <br> order="asc"\|"desc"
| **Description**| Search the database of posts and return the results with the page and page size specified, as well as searching within a specific field and with sorting options. <br> "pageSize": the amount of results you want returned in each call. <br> "start": the index of the first post returned. <br> "field": to search within a certain field. <br> "order": to return the results in ascending or descending order |
| **Example**    | posts/search/literacy/10/0/title/asc

### POST

| **Title**      | Create a Post    |
| :---------:    | ------  |
| **URL**        | /posts/create|
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` title:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`email:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`license:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`description:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`dateCreated:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsConsent:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsRelease:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorAuthor:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorInterviewer:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` creatorGender:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorRaceEthnicity:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorClass:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorYearOfBirth:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageSpatial:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coveragePeriod:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageRegion:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` coverageStateProvince:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageNationality:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`language:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`subject:[textarray]`<br>`}` |
| **Description**| Create a post by supplying it with details about the literacy narrative. Once called, the post will exist in the database. <br> Required values: <br> tableName, title, email, license |
| **Example**    | `{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` "title":"Shakib's Literacy Narrative",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"email":"shakib.r.ahmed@gmail.com",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"license":"Creative Commons",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"description":"This is my narrative.",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"dateCreated"="06/13/2017",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsConsent=adult,`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsRelease=adult,`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorAuthor=["Shakib Ahmed"],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorInterviewer=["Wasfi Momen", "Jaro Klc"],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` creatorGender=["male"],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorRaceEthnicity=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorClass=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorYearOfBirth=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageSpatial=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coveragePeriod=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageRegion=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` coverageStateProvince=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageNationality=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`language=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`subject=[textarray]`<br>`}` |


| **Title**      | Get Pre-signed URL for an S3 Upload       |
| :---------:    | ------  |
| **URL**        | /asset/s3uploader |
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`objectKey:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contentType:[string]`<br>`}`|
| **Description**| Supply the url with the object key (name of the file you want to upload to S3) and the content type for that file. |
| **Example**    |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"objectKey":"myfile.txt",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"contentType":"text-plain"`<br>`}`|

| **Title**      | Upload an Asset      |
| :---------:    | ------  |
| **URL**        | /asset/apiupload |
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`stagingAreaBucketName:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`finalBucketName:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`key:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`PostId:[string],` <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`queueName:[string],`               <br>`}`|
| **Description**| Enter the details for a file you want to upload an associate with a post. This file (which should already exist in the staging area bucket) will be sent as a message to the queue which is processed by an independent worker.|
| **Example** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"stagingAreaBucketName":"daln-file-staging-area-sb",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"finalBucketName":"daln-development",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"key":"myfile.txt",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"PostId":"d7fea027-152f-43da-a0d3-dd476c7164e5",` <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"queueName":"DALNFileUploadQueueDev",`               <br>`}`|

| **Title**      | Get Unapproved Posts |
| :---------:    | ------  |
| **URL**        | /admin/unapproved |
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string]`<br>`}`|
| **Description**| Return all posts that are not approved. All newly created posts are initially not approved. |
| **Example** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev"`<br>`}`|

| **Title**      | Approve a Post for the Search Engine   |
| :---------:    | ------  |
| **URL**        | /admin/approve |
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`postId:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string]`<br>`}`|
| **Description**| Approving a post will enter it into the search engine and will be publicly searchable. |
| **Example**    |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"postId":"d7fea027-152f-43da-a0d3-dd476c7164e5",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev"`<br>`}`|

| **Title**      | Remove a Post for the Search Engine (Unapprove) |
| :---------:    | ------  |
| **URL**        | /admin/remove |
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`postId:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string]`<br>`}`|
| **Description**| Removing/Unapproving a post will remove it into the search engine. The post will still exist as a DynamoDB record. |
| **Example**    |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"postId":"d7fea027-152f-43da-a0d3-dd476c7164e5",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev"`<br>`}`|

