package com.threadhall.moderation;

import com.threadhall.common.Channels;
import com.threadhall.common.ForumRules;
import com.threadhall.common.ModerateCommand;
import com.threadhall.common.ModerationEvent;
import com.threadhall.common.NoticeCommand;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ModerationWorker {

    private final DecisionRepository decisions;
    private final KafkaTemplate<String, Object> kafka;
    private final RabbitTemplate rabbit;

    public ModerationWorker(DecisionRepository decisions, KafkaTemplate<String, Object> kafka, RabbitTemplate rabbit) {
        this.decisions = decisions;
        this.kafka = kafka;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = Channels.MODERATE, concurrency = "4")
    @Transactional
    public void onModerate(ModerateCommand command) {
        if (decisions.existsById(command.postId())) {
            return;
        }
        String decision = ForumRules.decision(command.body());
        Publish.kafka(kafka, Channels.MODERATION, command.postId(),
                new ModerationEvent(command.postId(), command.threadId(), command.kind(), decision));
        if (!"VISIBLE".equals(decision)) {
            throw new AmqpRejectAndDontRequeueException(decision);
        }
        rabbit.convertAndSend(Channels.NOTICE, new NoticeCommand(
                command.postId(), command.threadId(), command.kind(), command.authorName()));
        DecisionEntity row = new DecisionEntity();
        row.setPostId(command.postId());
        row.setStatus(decision);
        decisions.save(row);
    }
}
