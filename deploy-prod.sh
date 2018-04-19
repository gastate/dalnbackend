mvn deploy -f ./pom-prod.xml

sleep 10
aws apigateway put-rest-api --rest-api-id iu2lnr6wyc --mode overwrite --body "file://./org.lambadaframework.thedaln-production-production-swagger-apigateway.yaml"
aws apigateway create-deployment --rest-api-id iu2lnr6wyc --stage-name production

