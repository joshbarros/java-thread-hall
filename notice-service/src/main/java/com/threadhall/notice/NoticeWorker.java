package com.threadhall.notice;

import com.threadhall.common.Channels;
import com.threadhall.common.ForumRules;
import com.threadhall.common.NoticeCommand;
import com.threadhall.common.NoticeEvent;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NoticeWorker {

    private final NoticeRepository notices;
    private final KafkaTemplate<String, Object> kafka;

    public NoticeWorker(NoticeRepository notices, KafkaTemplate<String, Object> kafka) {
        this.notices = notices;
        this.kafka = kafka;
    }

    @RabbitListener(queues = Channels.NOTICE, concurrency = "4")
    @Transactional
    public void onNotice(NoticeCommand command) {
        if (notices.existsById(command.postId())) {
            return;
        }
        if (ForumRules.bounce(command.authorName())) {
            Publish.kafka(kafka, Channels.NOTICES, command.postId(),
                    new NoticeEvent(command.postId(), command.threadId(), command.kind(), "BOUNCED"));
            throw new AmqpRejectAndDontRequeueException("undeliverable author");
        }
        NoticeEntity row = new NoticeEntity();
        row.setPostId(command.postId());
        row.setAuthorName(command.authorName());
        row.setStatus("DELIVERED");
        notices.save(row);
        Publish.kafka(kafka, Channels.NOTICES, command.postId(),
                new NoticeEvent(command.postId(), command.threadId(), command.kind(), "DELIVERED"));
    }
}
