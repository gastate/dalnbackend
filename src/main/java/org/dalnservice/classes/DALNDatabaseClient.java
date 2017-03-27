package org.dalnservice.classes;

/**
 * Created by Shakib on 2/8/2017.
 */
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Shakib on 8/4/2016.
 */
public class DALNDatabaseClient
{
    private AmazonCloudSearchDomainClient searchClient;
    private DynamoDBMapper mapper;
    private Table table;
    private String title, description, dateCreated, rightsConsent, rightsRelease;
    private String postId, identifierUri, dateAccessioned, dateAvailable, dateIssued; //may not all be needed
    private List<String> contributorAuthor, contributorInterviewer, creatorGender,
            creatorRaceEthnicity, creatorClass, creatorYearOfBirth, coverageSpatial,
            coveragePeriod, coverageRegion, coverageStateProvince, coverageNationality,
            language,subject;

    public DALNDatabaseClient() throws IOException {
        /**Authenticate clients and mapper**/
        //EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        //AWSCredentials awsCredentials = creds.getCredentials();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getenv("AWSAccessKey"), System.getenv("AWSSecretKey"));

        AmazonDynamoDB dynamoDBClient = new AmazonDynamoDBClient(awsCreds);
        mapper = new DynamoDBMapper(dynamoDBClient);

        DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCreds));
        table = dynamoDB.getTable("DALN-Posts");

        searchClient = new AmazonCloudSearchDomainClient(awsCreds);
        searchClient.setEndpoint(System.getenv("searchEndpoint"));
    }

    private void initializeCurrentPostAttributes(Post post)
    {
        //If the post attribute is null, then create a new string/arraylist, else get the current value
        title = (post.getTitle()==null)? "" : post.getTitle();
        description = (post.getDescription()==null)? "" : post.getDescription();
        dateCreated = (post.getDateCreated()==null)? "" : post.getDateCreated();
        rightsRelease = (post.getRightsRelease()==null)? "" : post.getRightsRelease();
        rightsConsent = (post.getRightsConsent()==null)? "" : post.getRightsConsent();
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

    private void updatePostAttributes(Post post)
    {
        post.setTitle(title);
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

    public String createPost(String title)
    {
        Post post = new Post();
        post.setTitle(title);

        //Enter it into the DB
        mapper.save(post);

        return post.getPostId(); //return the UUID generated from the insertion into DB
    }


    public Post getPost(String postId)
    {
        return mapper.load(Post.class, postId);
    }


    public void updatePost(String postID, JSONObject input)
    {
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



    public void uploadAsset(String postId, HashMap<String, String> assetDetails)
    {
        Post post = mapper.load(Post.class, postId);
        List<HashMap<String, String>> assetList = (post.getAssetList() == null) ? new ArrayList<HashMap<String, String>>() : post.getAssetList();
        assetList.add(assetDetails);
        post.setAssetList(assetList);

        mapper.save(post);

    }


    public List<Post> getAllPosts()
    {

        return mapper.scan(Post.class, new DynamoDBScanExpression());
    }

    public List<Post> getPostsFromDaysAgoUntilNow(int daysAgo)
    {
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

    public List<Post> getRandomSet(int size)
    {
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

    public List<Post> getPageScan(int pageSize, int page)
    {
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

    //Simple search
    public List<Post> search(String query) throws ParseException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setReturn("_no_fields");

        SearchResult searchResult = searchClient.search(searchRequest);
        List<Post> posts = new ArrayList<>();

        Hits searchHits = searchResult.getHits();
        List<Hit> hitList = searchHits.getHit();


        for(Hit hit : hitList)
        {
            String postID = hit.getId();
            System.out.println("search result:" + postID);
            posts.add(mapper.load(Post.class, postID));
        }

        return posts;
    }

    //Simple search with pagination
    public List<Post> search(String query, long pageSize, long hitStart) throws ParseException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setReturn("_no_fields");
        searchRequest.setSize(pageSize);
        searchRequest.setStart(hitStart);

        SearchResult searchResult = searchClient.search(searchRequest);
        List<Post> posts = new ArrayList<>();

        Hits searchHits = searchResult.getHits();
        List<Hit> hitList = searchHits.getHit();


        for(Hit hit : hitList)
        {
            String postID = hit.getId();
            posts.add(mapper.load(Post.class, postID));
        }

        return posts;
    }

    //Sorted search with pagination
    public List<Post> search(String query, long pageSize, long hitStart, String fieldToSortBy, String order) throws ParseException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setReturn("_no_fields");
        searchRequest.setSize(pageSize);
        searchRequest.setStart(hitStart);
        searchRequest.setSort(fieldToSortBy + " " + order);

        SearchResult searchResult = searchClient.search(searchRequest);
        List<Post> posts = new ArrayList<>();

        Hits searchHits = searchResult.getHits();
        List<Hit> hitList = searchHits.getHit();


        for(Hit hit : hitList)
        {
            String postID = hit.getId();
            posts.add(mapper.load(Post.class, postID));
        }

        return posts;
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



    public void destroy() {
        com.amazonaws.http.IdleConnectionReaper.shutdown();
    }



}
