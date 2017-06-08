# DALN REST API

This is the REST API for the DALN. It is handled through AWS Lambda and API Gateway.

## REST Functions

### GET

| **Title**      | Get a Single Post (with Path Parameter)        |
| :---------:    | ------  |
| **URL**        | /posts/get/:id |
| **URL Params** | Required: <br> id=[string] |
| **Description**| Supply the url with the post ID and have the individual post returned. |
| **Example**    | /posts/get/3b983676-d935-4a2c-b537-4becd5a545b5 |


Title: Get a Single Post (with Path Parameter) <br>
URL: /posts/get/:id <br>
URL Params:
* Required: id=[string]

Description: Supply the url with the post ID and have the individual post returned.
Example: /posts/get/3b983676-d935-4a2c-b537-4becd5a545b5

Title: Get a Single Post (with Form Parameter)

Title: Get All Posts
URL: /posts/all
Description: Have all posts in the database returned.

Title: Get Posts from Days Ago

Title: Get a Random Selection of Posts

Title: Get Posts with Pagination

Title: Search <br>
URL: /posts/search/:query/:pageSize/:start/:field/:order <br>
URL Params: <br>
* Required: query=[string]
* Optional: <br>
pageSize=[integer] <br>
              start=[integer] <br>
              field=[string]
              order="asc"|"desc"
Description: Search the database of posts and return the results.
    If only the query is specified, all results will be returned.
    "pageSize": the amount of results you want returned in each call.
    "start": the index of the first post returned.
    "field": to search within a certain field.
    "order": to return the results in ascending or descending order
Example: /posts/search/literacy
         /posts/search/literacy/10/0
         /posts/search/literacy/10/0/title/asc

Title: Get Pre-signed URL for an S3 Upload
URL: /asset/s3upload/:key
URL Params:
    Required: key=[string]
Description: Supply the url with the object key (name of the file you want to upload to S3).
Example: /asset/s3upoad/my file.txt

### POST

Title: Create a Post
URL: /posts/create
Data Params:
    Required: title=[string]
    Optional: description=[string]
              dateCreated=[string]
              rightsConsent=[string]
              rightsRelease=[string]
              contributorAuthor=[textarray]
              contributorInterviewer=[textarray]
              creatorGender=[textarray]
              creatorRaceEthnicity=[textarray]
              creatorClass=[textarray]
              creatorYearOfBirth=[textarray]
              coverageSpatial=[textarray]
              coveragePeriod=[textarray]
              coverageRegion=[textarray]
              coverageStateProvince=[textarray]
              coverageNationality=[textarray]
              language=[textarray]
              subject=[textarray]
Description: Create a post by supplying it with details about the literacy narrative. A post only requires the title.
Once called, the post will exist in the database.
Example:
    {
    "contributorInterviewer": [
        "Jensen, Timothy"
      ],
      "coverageNationality": [
        "American"
      ],
      "coveragePeriod": [
        "1980-1989",
        "1990-1999",
        "2000-2009"
      ],
      "coverageRegion": [
        "Mid-West"
      ],
      "coverageSpatial": [
        "St. Louis"
      ],
      "coverageStateProvince": [
        "Ohio"
      ],
      "creatorClass": [
        "middle class"
      ],
      "creatorGender": [
        "Female"
      ],
      "creatorRaceEthnicity": [
        "White"
      ],
      "creatorYearOfBirth": [
        "1982"
      ],
      "dateCreated": "2008-09-14",
      "description": "Kristin, a graphic designer and graduate student in literary studies, discusses her literacy development from childhood on.",
      "language": [
        "English"
      ],
      "rightsConsent": "adult",
      "rightsRelease": "adult",
      "subject": [
        "mid-west",
        "home-school"
      ],
      "title": "Timothy's Literacy Narrative"
    }

Title: Upload an Asset

Title: Approve a Post for the Search Engine

Title: Remove a Post from the Search Engine
