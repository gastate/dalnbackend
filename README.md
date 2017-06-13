# DALN REST API

This is the REST API for the DALN. It is handled through AWS Lambda and API Gateway.

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



| **Title**      | Get Pre-signed URL for an S3 Upload       |
| :---------:    | ------  |
| **URL**        | /asset/s3upload/:key |
| **URL Params** | Required: <br> key=[string] |
| **Description**| Supply the url with the object key (name of the file you want to upload to S3). |
| **Example**    | /asset/s3upload/my file.txt |

### POST

| **Title**      | Create a Post    |
| :---------:    | ------  |
| **URL**        | /posts/create|
| **Data Params** |`{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`tableName:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` title:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`email:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`license:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`description:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`dateCreated:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsConsent:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsRelease:[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorAuthor:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorInterviewer:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` creatorGender:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorRaceEthnicity:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorClass:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorYearOfBirth:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageSpatial:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coveragePeriod:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageRegion:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` coverageStateProvince:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageNationality:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`language:[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`subject:[textarray]`<br>`}` |
| **Description**| Create a post by supplying it with details about the literacy narrative. Once called, the post will exist in the database. <br> Required values: <br> tableName, title, email, license |
| **Example**    | `{ `<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"tableName":"DALN-Posts-Dev",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` "title":"Shakib's Literacy Narrative",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"email":"shakib.r.ahmed@gmail.com",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"license":"Creative Commons",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"description":"This is my narrative.",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`"dateCreated"="06/13/2017",`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsConsent=[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`rightsRelease=[string],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorAuthor=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`contributorInterviewer=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` creatorGender=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorRaceEthnicity=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorClass=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`creatorYearOfBirth=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageSpatial=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coveragePeriod=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageRegion=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` coverageStateProvince=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`coverageNationality=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`language=[textarray],`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`subject=[textarray]`<br>`}` |



Title: Upload an Asset

Title: Approve a Post for the Search Engine

Title: Remove a Post from the Search Engine
