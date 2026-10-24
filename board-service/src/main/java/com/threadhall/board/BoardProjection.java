package com.threadhall.board;

import com.threadhall.common.Channels;
import com.threadhall.common.ModerationEvent;
import com.threadhall.common.NoticeEvent;
import jakarta.persistence.EntityManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BoardProjection {

    private final EntityManager em;

    public BoardProjection(EntityManager em) {
        this.em = em;
    }

    @KafkaListener(topics = Channels.MODERATION, groupId = "board-moderation")
    @Transactional
    public void onModeration(ModerationEvent event) {
        if ("THREAD".equals(event.kind())) {
            em.createNativeQuery("UPDATE threads SET status = :status WHERE id = :id AND status = 'PENDING'")
                    .setParameter("status", event.status())
                    .setParameter("id", event.postId())
                    .executeUpdate();
            return;
        }
        if (!"REPLY".equals(event.kind())) {
            return;
        }
        int updated = em.createNativeQuery("UPDATE replies SET status = :status WHERE id = :id AND status = 'PENDING'")
                .setParameter("status", event.status())
                .setParameter("id", event.postId())
                .executeUpdate();
        if (updated == 1 && "VISIBLE".equals(event.status())) {
            int inserted = em.createNativeQuery("INSERT INTO counted_replies (reply_id) VALUES (:id) ON CONFLICT DO NOTHING")
                    .setParameter("id", event.postId())
                    .executeUpdate();
            if (inserted == 1) {
                em.createNativeQuery("UPDATE threads SET reply_count = reply_count + 1 WHERE id = :id")
                        .setParameter("id", event.threadId())
                        .executeUpdate();
            }
        }
    }

    @KafkaListener(topics = Channels.NOTICES, groupId = "board-notices")
    @Transactional
    public void onNotice(NoticeEvent event) {
        String sql = "THREAD".equals(event.kind())
                ? "UPDATE threads SET notice_status = :status WHERE id = :id"
                : "UPDATE replies SET notice_status = :status WHERE id = :id";
        em.createNativeQuery(sql)
                .setParameter("status", event.status())
                .setParameter("id", event.postId())
                .executeUpdate();
    }
}
