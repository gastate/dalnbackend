mvn deploy -f ./pom-prod.xml

sleep 10
aws apigateway put-rest-api --rest-api-id w7b01d4a3g --mode overwrite --body "file://./org.lambadaframework.thedaln-production-production-swagger-apigateway.yaml"
aws apigateway create-deployment --rest-api-id w7b01d4a3g --stage-name production

