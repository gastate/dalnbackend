package org.dalnservice.classes;

/**
 * Created by Shakib on 2/8/2017.
 */
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Shakib on 8/4/2016.
 */
public class DALNDatabaseClient
{
    private AmazonDynamoDB dynamoDBClient;
    private AmazonCloudSearchDomainClient searchClient;
    private DALNCloudSearchClient searchDocumentManager;
    private DynamoDBMapper mapper;
    private Table table;
    private String tableName;
    private String title, email, license, description, dateCreated, rightsConsent, rightsRelease, dateSubmitted, dateIssued;
    private String postId, identifierUri, dateAccessioned, dateAvailable; //may not all be needed
    private List<String> contributorAuthor, contributorInterviewer, creatorGender,
            creatorRaceEthnicity, creatorClass, creatorYearOfBirth, coverageSpatial,
            coveragePeriod, coverageRegion, coverageStateProvince, coverageNationality,
            language,subject;
    private boolean isPostNotApproved;

    public DALNDatabaseClient() throws IOException {
        /**Authenticate clients and mapper**/
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCreds = creds.getCredentials();
        //BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getenv("AWSAccessKey"), System.getenv("AWSSecretKey"));

        dynamoDBClient = new AmazonDynamoDBClient(awsCreds);
        mapper = new DynamoDBMapper(dynamoDBClient);
        updateTableName("DALN-Posts");

        //DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCreds));
        //table = dynamoDB.getTable("DALN-Posts-Dev");

        searchClient = new AmazonCloudSearchDomainClient(awsCreds);
        searchClient.setEndpoint(System.getenv("searchEndpoint"));

        searchDocumentManager = new DALNCloudSearchClient();
    }

    /**HELPER FUNCTIONS
     * The following methods assist in other functions in this class and do not make direct changes to the records in the table.
     */
    public void updateTableName(String tableName)
    {
        DynamoDBMapperConfig mapperConfig =
                new DynamoDBMapperConfig.Builder().withTableNameOverride(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                        .build();

        mapper = new DynamoDBMapper(dynamoDBClient, mapperConfig);
    }

    private void initializeCurrentPostAttributes(Post post)
    {
        //If the post attribute is null, then create a new string/arraylist, else get the current value
        title = (post.getTitle()==null)? "" : post.getTitle();
        email = (post.getEmail()==null)? "" : post.getEmail();
        license = (post.getLicense()==null)? "" : post.getLicense();
        description = (post.getDescription()==null)? "" : post.getDescription();
        dateCreated = (post.getDateCreated()==null)? "" : post.getDateCreated();
        rightsRelease = (post.getRightsRelease()==null)? "" : post.getRightsRelease();
        rightsConsent = (post.getRightsConsent()==null)? "" : post.getRightsConsent();
        isPostNotApproved = post.getIsPostNotApproved();
        contributorAuthor = (post.getContributorAuthor()==null)? new ArrayList<String>() : post.getContributorAuthor();
        contributorInterviewer = (post.getContributorInterviewer()==null)? new ArrayList<String>() : post.getContributorInterviewer();
        creatorGender = (post.getCreatorGender()==null)? new ArrayList<String>() : post.getCreatorGender();
        creatorRaceEthnicity = (post.getCreatorRaceEthnicity()==null)? new ArrayList<String>() : post.getCreatorRaceEthnicity();
        creatorClass = (post.getCreatorClass()==null)? new ArrayList<String>() : post.getCreatorClass();
        creatorYearOfBirth = (post.getCreatorYearOfBirth()==null)? new ArrayList<String>() : post.getCreatorYearOfBirth();
        coverageSpatial = (post.getCoverageSpatial()==null)? new ArrayList<String>() : post.getCoverageSpatial();
        coveragePeriod = (post.getCoveragePeriod()==null)? new ArrayList<String>() : post.getCoveragePeriod();
        coverageRegion = (post.getCoverageRegion()==null)? new ArrayList<String>() : post.getCoverageRegion();
        coverageStateProvince = (post.getCoverageStateProvince()==null)? new ArrayList<String>() : post.getCoverageStateProvince();
        coverageNationality = (post.getCoverageNationality()==null)? new ArrayList<String>() : post.getCoverageNationality();
        language = (post.getLanguage()==null)? new ArrayList<String>() : post.getLanguage();
        subject = (post.getSubject()==null)? new ArrayList<String>() : post.getSubject();

    }

    private void deleteEmptyAttributes()
    {
        if(title.isEmpty()) title = null;
        if(email.isEmpty()) email = null;
        if(license.isEmpty()) license = null;
        if(description.isEmpty()) description = null;
        if(dateCreated.isEmpty()) dateCreated = null;
        if(rightsConsent.isEmpty()) rightsConsent = null;
        if(rightsRelease.isEmpty()) rightsRelease = null;
        if(contributorAuthor.isEmpty()) contributorAuthor = null;
        if(contributorInterviewer.isEmpty()) contributorInterviewer = null;
        if(creatorGender.isEmpty()) creatorGender = null;
        if(creatorRaceEthnicity.isEmpty()) creatorRaceEthnicity = null;
        if(creatorClass.isEmpty()) creatorClass = null;
        if(creatorYearOfBirth.isEmpty()) creatorYearOfBirth = null;
        if(coverageSpatial.isEmpty()) coverageSpatial = null;
        if(coveragePeriod.isEmpty()) coveragePeriod = null;
        if(coverageRegion.isEmpty()) coverageRegion = null;
        if(coverageStateProvince.isEmpty()) coverageStateProvince = null;
        if(coverageNationality.isEmpty()) coverageNationality = null;
        if(language.isEmpty()) language = null;
        if(subject.isEmpty()) subject = null;
    }


    /**TABLE UPDATE FUNCTIONS
     * The following functions update the table by either creating a new record or updating a current record.
     */
    public String createPost(String tableName, String title, String email, String license)
    {
        updateTableName(tableName);
        Post post = new Post();
        post.setTitle(title);
        post.setEmail(email);
        post.setLicense(license);
        post.setIsPostNotApproved(true);
        post.setAreAllFilesUploaded(false);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        Date date = new Date();
        String dateSubmitted = dateFormat.format(date);
        post.setDateSubmitted(dateSubmitted);

        //Enter it into the DB
        mapper.save(post);

        return post.getPostId(); //return the UUID generated from the insertion into DB
    }

    public void updatePost(String tableName, String postID, JSONObject input)
    {
        updateTableName(tableName);
        Post post = mapper.load(Post.class, postID);
        initializeCurrentPostAttributes(post);

        for(Object key : input.keySet())
        {
            Object value = input.get(key);
            if(value==null || value.equals(""))
                continue;

            if (key.equals("contributorAuthor"))
                contributorAuthor.addAll((ArrayList)value);
            else if (key.equals("contributorInterviewer"))
                contributorInterviewer.addAll((ArrayList)value);
            else if (key.equals("creatorGender"))
                creatorGender.addAll((ArrayList)value);
            else if (key.equals("creatorRaceEthnicity"))
                creatorRaceEthnicity.addAll((ArrayList)value);
            else if (key.equals("creatorClass"))
                creatorClass.addAll((ArrayList)value);
            else if (key.equals("creatorYearOfBirth"))
                creatorYearOfBirth.addAll((ArrayList)value);
            else if (key.equals("coverageSpatial"))
                coverageSpatial.addAll((ArrayList)value);
            else if (key.equals("coveragePeriod"))
                coveragePeriod.addAll((ArrayList)value);
            else if (key.equals("coverageRegion"))
                coverageRegion.addAll((ArrayList)value);
            else if (key.equals("coverageStateProvince"))
                coverageStateProvince.addAll((ArrayList)value);
            else if (key.equals("coverageNationality"))
                coverageNationality.addAll((ArrayList)value);
            else if (key.equals("language"))
                language.addAll((ArrayList)value);
            else if (key.equals("subject"))
                subject.addAll((ArrayList)value);
            else if (key.equals("title"))
                title = value.toString();
            else if (key.equals("email"))
                email = value.toString();
            else if (key.equals("license"))
                license = value.toString();
            else if (key.equals("description"))
                description = value.toString();
            else if (key.equals("rightsConsent"))
                rightsConsent = value.toString();
            else if (key.equals("rightsRelease"))
                rightsRelease = value.toString();
            else if (key.equals("dateCreated"))
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date = dateFormat.parse(value.toString());
                    dateCreated = dateFormat.format(date);
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        deleteEmptyAttributes();
        updatePostAttributes(post);
    }

    private void updatePostAttributes(Post post)
    {
        post.setTitle(title);
        post.setEmail(email);
        post.setLicense(license);
        post.setIsPostNotApproved(isPostNotApproved);
        post.setDescription(description);
        post.setDateCreated(dateCreated);
        post.setRightsRelease(rightsRelease);
        post.setRightsConsent(rightsConsent);
        post.setContributorAuthor(contributorAuthor);
        post.setContributorInterviewer(contributorInterviewer);
        post.setCreatorGender(creatorGender);
        post.setCreatorRaceEthnicity(creatorRaceEthnicity);
        post.setCreatorClass(creatorClass);
        post.setCreatorYearOfBirth(creatorYearOfBirth);
        post.setCoverageSpatial(coverageSpatial);
        post.setCoveragePeriod(coveragePeriod);
        post.setCoverageRegion(coverageRegion);
        post.setCoverageStateProvince(coverageStateProvince);
        post.setCoverageNationality(coverageNationality);
        post.setLanguage(language);
        post.setSubject(subject);

        mapper.save(post);
    }

    public void uploadAsset(String tableName,String postId, HashMap<String, String> assetDetails)
    {
        updateTableName(tableName);
        Post post = mapper.load(Post.class, postId);
        List<HashMap<String, String>> assetList = (post.getAssetList() == null) ? new ArrayList<HashMap<String, String>>() : post.getAssetList();
        assetList.add(assetDetails);
        post.setAssetList(assetList);

        mapper.save(post);

    }

    public void deletePost(String tableName, String postId)
    {
        updateTableName(tableName);
        mapper.delete(mapper.load(Post.class, postId));
    }

    /**GET FUNCTIONS
     * The following functions access the table in the DB and returns posts.
     * **/
    public Post getPost(String tableName, String postId)
    {
        updateTableName(tableName);
        return mapper.load(Post.class, postId);
    }


    //from main daln table
    public List<Post> getAllPosts()
    {

        updateTableName("DALN-Posts");
        return mapper.scan(Post.class, new DynamoDBScanExpression());
    }


    //from main daln table
    public List<Post> getPostsFromDaysAgoUntilNow(int daysAgo)
    {
        updateTableName("DALN-Posts");

        Date today = new Date();
        long daysAgoMilli = (new Date()).getTime() - ((long)daysAgo*24L*60L*60L*1000L);
        Date daysAgoDate = new Date();
        daysAgoDate.setTime(daysAgoMilli);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String daysAgoStr = df.format(daysAgoDate);
        String todayStr = df.format(today);

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":v1",new AttributeValue().withS(daysAgoStr));
        eav.put(":v2", new AttributeValue().withS(todayStr));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withConsistentRead(false)
                .withFilterExpression("dateCreated BETWEEN :v1 AND :v2")
                .withProjectionExpression("PostId, dateCreated")
                .withExpressionAttributeValues(eav);

        return mapper.scan(Post.class, scanExpression);
    }

    //from main daln table
    public List<Post> getRandomSet(int size)
    {
        updateTableName("DALN-Posts");

        AttributeValue randomUUID;

        Map<String, AttributeValue> lastEvalKey = new HashMap<String, AttributeValue>();
        List<Post> randomPosts = new ArrayList<Post>();
        for(int i = 0; i < size; i++)
        {
            randomUUID = new AttributeValue().withS(UUID.randomUUID().toString());
            lastEvalKey.put("PostId", randomUUID);

            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withProjectionExpression("PostId")
                    .withExclusiveStartKey(lastEvalKey)
                    .withConsistentRead(false)
                    .withLimit(1);

            List<Post> randomPost = new ArrayList<>();
            do {
                ScanResultPage<Post> scannedResult = mapper.scanPage(Post.class, scanExpression);
                randomPost = scannedResult.getResults();
                randomPosts.add(randomPost.get(0));
            }
            while(!randomPosts.contains(randomPost.get(0)));
        }

        return randomPosts;
    }

    //set to delete page scan
    public List<Post> getPageScan(int pageSize, int page)
    {
        updateTableName("DALN-Posts");

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withLimit(pageSize)
                .withProjectionExpression("PostId");

        Map<String, AttributeValue> lastEvalKey = null;
        List<Post> results = new ArrayList<>();
        int pagesScanned = 0;

        do
        {
            ScanResultPage<Post> scanPage = mapper.scanPage(Post.class, scanExpression);
            lastEvalKey = scanPage.getLastEvaluatedKey();
            results.clear();
            results = scanPage.getResults();
            scanExpression.setExclusiveStartKey(lastEvalKey);
            pagesScanned++;
        }
        while(pagesScanned < page);

        return results;
    }

    public List<Post> getUnapprovedPosts(String tableName)
    {
        updateTableName(tableName);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1",new AttributeValue().withN("1"));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("isPostNotApproved = :v1")
                .withExpressionAttributeValues(eav);
        List<Post> scanResult = mapper.scan(Post.class, scanExpression);

        return scanResult;
    }

    public String getPostIDFromTableUsingDALNId(String dalnId)
    {
        Post post = new Post();
        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(dalnId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("dalnId = :val1")
                .withExpressionAttributeValues(eav);

        List<Post> scanResults = mapper.scan(Post.class, scanExpression);
        if(scanResults.size() == 0) //post was not found (doesn't exist)
            return null;
        else
            post = scanResults.get(0);

        return post.getPostId();
    }



    public void getFilesUploadedInfo(String tableName, int totalNumberOfFiles)
    {
        updateTableName(tableName);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1",new AttributeValue().withN("0"));
        eav.put(":v2",new AttributeValue().withN("1"));

        //find posts that are not approved and not all the files are uploaded (mainly new posts)
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("areAllFilesUploaded = :v1 and isPostNotApproved = :v2")
                .withExpressionAttributeValues(eav);
        List<Post> scanResult = mapper.scan(Post.class, scanExpression);
        //look at every post and determine if all the files are uploaded for that post.
        //if yes, then change the value of areallFilesUploaded
        for(Post post : scanResult)
        {
            List<HashMap<String,String>> assetList = post.getAssetList();
            if(assetList.size() != totalNumberOfFiles)
            {
                if(assetList == null || assetList.size() == 0)
                {
                    System.out.println("No files have been uploaded yet");
                }
                //not all files are uploaded, figure out how many are
                for(HashMap<String,String> asset : assetList)
                {
                    System.out.println(asset.get("assetName") + " has been uploaded.");
                }
            }
            else
            {
                System.out.println("All of this post's assets have been uploaded");
            }

        }
    }



    /**SEARCH FUNCTIONS
     * The following functions utilize Amazon CloudSearch either return posts based on the query or update the search engine.
     * **/


    //Search returning the hits
    public Hits search(String query) throws ParseException
    {
        updateTableName("DALN-Posts");
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setQueryOptions("    {    \"defaultOperator\":\"or\"}" );
        searchRequest.setReturn("_all_fields");

        SearchResult searchResult = searchClient.search(searchRequest);
        Hits searchHits = searchResult.getHits();
        /*List<Hit> listOfHits = searchHits.getHit();

        JSONObject fullSearchResponse = new JSONObject();
        fullSearchResponse.put("found", searchHits.getFound());
        fullSearchResponse.put("start", searchHits.getStart());

        org.json.simple.JSONArray arrayOfPosts = new org.json.simple.JSONArray();

        ArrayList<Post> listOfPosts = new ArrayList<>();

        for(Hit result : listOfHits)
        {
            Post post = new Post();
            post.setPostId(result.getId());
            listOfPosts.add(post);
        }

        arrayOfPosts.addAll(mapper.batchLoad(listOfPosts).get("DALN-Posts"));
        fullSearchResponse.put("posts", arrayOfPosts);*/

        return searchHits;
    }

    //Search returning the hits
    public Hits search(String query, long pageSize, long hitStart) throws ParseException {
        updateTableName("DALN-Posts");

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setReturn("_all_fields");
        searchRequest.setSize(pageSize);
        searchRequest.setQueryOptions("    {    \"defaultOperator\":\"or\"}" );
        searchRequest.setStart(hitStart);

        SearchResult searchResult = searchClient.search(searchRequest);
        Hits searchHits = searchResult.getHits();

        return searchHits;
    }

    //Sorted search with pagination
    public Hits search(String query, long pageSize, long hitStart, String fieldToSortBy, String order) throws ParseException {
        updateTableName("DALN-Posts");

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setQueryOptions("    {    \"defaultOperator\":\"or\"}" );
        searchRequest.setReturn("_all_fields");
        searchRequest.setSize(pageSize);
        searchRequest.setStart(hitStart);
        searchRequest.setSort(fieldToSortBy + " " + order);

        SearchResult searchResult = searchClient.search(searchRequest);
        Hits searchHits = searchResult.getHits();
        return searchHits;
        //List<Hit> hitList = searchHits.getHit();


        //for(Hit hit : hitList)
        //{
         //   String postID = hit.getId();
         //   posts.add(mapper.load(Post.class, postID));
        //}
    }

    //Return the number of documents in search engine
    public long getSearchEngineSize() throws ParseException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery("matchall");
        searchRequest.setQueryParser("structured");

        SearchResult searchResult = searchClient.search(searchRequest);

        Hits searchHits = searchResult.getHits();

        return searchHits.getFound();
    }

    public boolean checkIfUUIDExists(String newUUID)
    {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(newUUID);
        searchRequest.setReturn("_no_fields");

        SearchResult searchResult = searchClient.search(searchRequest);
        if(searchResult.getHits().getHit().isEmpty()) //no results found
            return false;
        else
            return true;
        /*
        List<Post> allPosts = mapper.scan(Post.class, new DynamoDBScanExpression());
        for(Post post : allPosts)
            if(post.getAssetList() != null)
                for(HashMap<String, String> asset : post.getAssetList())
                    if(newUUID.equals(asset.get("Asset ID")))
                        return true;
        return false;
        */
    }

    //Enter a new document into the search engine
    public void enterPostIntoCloudSearch(String postIdToApprove, String tableName) throws IOException, ParseException {
        updateTableName(tableName);
        Post post = mapper.load(Post.class, postIdToApprove);
        post.setIsPostNotApproved(false);
        post.setAreAllFilesUploaded(true);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        Date date = new Date();
        String dateIssued = dateFormat.format(date);
        post.setDateIssued(dateIssued);

        mapper.save(post);
        JSONObject postAsSDF = searchDocumentManager.convertDynamoEntryToAddSDF(postIdToApprove, tableName);
        searchDocumentManager.uploadSingleDocument(postAsSDF);
    }

    //Remove a document from the search engine
    public void removePostFromCloudSearch(String postIdToRemove, String tableName) throws IOException, ParseException {
        updateTableName(tableName);
        Post post = mapper.load(Post.class, postIdToRemove);
        post.setIsPostNotApproved(true);
        mapper.save(post);
        JSONObject postAsSDF = searchDocumentManager.convertDynamoEntryToDeleteSDF(postIdToRemove);
        searchDocumentManager.uploadSingleDocument(postAsSDF);
    }


    /*

    public List<String> queryByDate(String date)
    {
        date = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6);
        //System.out.println(date);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1",new AttributeValue().withS(date));

        DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                .withKeyConditionExpression("dateCreated=:v1")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);

        QueryResultPage queryResultPage = mapper.queryPage(Post.class, queryExpression);

        return queryResultPage.getResults();
    }
*/



}
