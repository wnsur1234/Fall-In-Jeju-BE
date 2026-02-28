package com.jeju.ormicamp.common.config.bedrock;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AwsProperties {

    @Value("${aws.region}")
    private String DynamoRegion;

    @Value("${aws.dynamodb.table-name}")
    private String DynamoTableName;

    @Value("${aws.dynamodb.credentials.access-key}")
    private String accessKey;

    @Value("${aws.dynamodb.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.bedrock.api-gateway-url}")
    private String agentApiGatewayUrl;

}
