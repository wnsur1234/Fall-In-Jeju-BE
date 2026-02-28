//package com.jeju.ormicamp.common.config;
//
//import com.jeju.ormicamp.model.dto.map.RouteResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory(
//            @Value("${spring.data.redis.host}") String host,
//            @Value("${spring.data.redis.port}") int port
//    ) {
//        return new LettuceConnectionFactory(host, port);
//    }
//
//    @Bean
//    public RedisTemplate<String, RouteResponse> routeRedisTemplate(
//            RedisConnectionFactory connectionFactory
//    ) {
//        RedisTemplate<String, RouteResponse> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//
//        template.afterPropertiesSet();
//        return template;
//    }
//}
