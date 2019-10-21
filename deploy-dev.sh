mvn deploy -f ./pom-dev.xml

sleep 10
aws apigateway put-rest-api --rest-api-id h10w14u1wd --mode overwrite --body "file://./org.lambadaframework.thedaln-development-development-swagger-apigateway.yaml"
aws apigateway create-deployment --rest-api-id h10w14u1wd --stage-name development

