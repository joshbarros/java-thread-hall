package com.threadhall.board;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadhall.common.Channels;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class QueueConfig {

    @Bean
    public Queue moderateDlq() {
        return QueueBuilder.durable(Channels.MODERATE_DLQ).build();
    }

    @Bean
    @DependsOn("moderateDlq")
    public Queue moderateQueue() {
        return QueueBuilder.durable(Channels.MODERATE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.MODERATE_DLQ)
                .build();
    }

    @Bean
    public Queue noticeDlq() {
        return QueueBuilder.durable(Channels.NOTICE_DLQ).build();
    }

    @Bean
    @DependsOn("noticeDlq")
    public Queue noticeQueue() {
        return QueueBuilder.durable(Channels.NOTICE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.NOTICE_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
