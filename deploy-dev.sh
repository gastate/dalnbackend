mvn deploy -f ./pom-dev.xml

sleep 10
aws apigateway put-rest-api --rest-api-id 5odt95uhfg --mode overwrite --body "file://./org.lambadaframework.thedaln-development-development-swagger-apigateway.yaml"
aws apigateway create-deployment --rest-api-id 5odt95uhfg --stage-name development

