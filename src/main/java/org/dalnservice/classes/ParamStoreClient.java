package org.dalnservice.classes;

import java.io.IOException;
import java.util.HashMap;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

public class ParamStoreClient {
    public String getSproutVideoApiKey() throws IOException {

        String sproutVideoApiKey = "";
        try {
            AWSSimpleSystemsManagement simpleSystemsManagementClient = AWSSimpleSystemsManagementClientBuilder
                    .standard().build();
            GetParameterRequest parameterRequest = new GetParameterRequest();
            parameterRequest.withName("/daln/SproutVideoApiKey").setWithDecryption(true);
            GetParameterResult parameterResult = simpleSystemsManagementClient.getParameter(parameterRequest);

            sproutVideoApiKey = parameterResult.getParameter().getValue();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return sproutVideoApiKey;
    }

    public HashMap<String, String> getSoundCloudClientInfo() throws IOException {
        HashMap<String, String> soundCloudClientInfo = new HashMap<>();

        try {
            AWSSimpleSystemsManagement simpleSystemsManagementClient = AWSSimpleSystemsManagementClientBuilder
                    .standard().build();
            GetParametersRequest parametersRequest = new GetParametersRequest();
            parametersRequest.withNames("/daln/SoundCloudClientID", "/daln/SoundCloudClientSecret",
                    "/daln/SoundCloudUser", "/daln/SoundCloudPassword").setWithDecryption(true);
            GetParametersResult parameterResult = simpleSystemsManagementClient.getParameters(parametersRequest);
            for (Parameter param : parameterResult.getParameters()) {
                soundCloudClientInfo.put(param.getName().replace("/daln/", ""), param.getValue());
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return soundCloudClientInfo;
    }
}