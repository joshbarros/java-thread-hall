package com.threadhall.board;

import java.util.UUID;

import com.threadhall.common.Channels;
import com.threadhall.common.ModerateCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BoardController {

    private final BoardRepository boards;
    private final ThreadRepository threads;
    private final ReplyRepository replies;
    private final RabbitTemplate rabbit;

    public BoardController(BoardRepository boards, ThreadRepository threads, ReplyRepository replies, RabbitTemplate rabbit) {
        this.boards = boards;
        this.threads = threads;
        this.replies = replies;
        this.rabbit = rabbit;
    }

    @PostMapping("/boards")
    public ResponseEntity<BoardEntity> createBoard(@RequestBody CreateBoardRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        BoardEntity board = new BoardEntity();
        board.setId(UUID.randomUUID().toString());
        board.setName(request.name().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(boards.save(board));
    }

    @PostMapping("/boards/{id}/threads")
    @Transactional
    public ResponseEntity<ThreadEntity> createThread(@PathVariable String id, @RequestBody CreateThreadRequest request) {
        if (!boards.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }
        if (request == null || blank(request.authorName()) || blank(request.title()) || blank(request.body())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorName, title, and body are required");
        }
        ThreadEntity thread = new ThreadEntity();
        thread.setId(UUID.randomUUID().toString());
        thread.setBoardId(id);
        thread.setAuthorName(request.authorName().trim());
        thread.setTitle(request.title().trim());
        thread.setBody(request.body());
        thread.setStatus("PENDING");
        thread.setNoticeStatus("NONE");
        thread.setReplyCount(0);
        threads.save(thread);
        Publish.afterCommit(() -> rabbit.convertAndSend(Channels.MODERATE, new ModerateCommand(
                thread.getId(), thread.getId(), "THREAD", thread.getAuthorName(), thread.getBody())));
        return ResponseEntity.status(HttpStatus.CREATED).body(thread);
    }

    @PostMapping("/threads/{id}/replies")
    @Transactional
    public ResponseEntity<ReplyEntity> reply(@PathVariable String id, @RequestBody ReplyRequest request) {
        if (!threads.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found");
        }
        if (request == null || blank(request.authorName()) || blank(request.body())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorName and body are required");
        }
        ReplyEntity reply = new ReplyEntity();
        reply.setId(UUID.randomUUID().toString());
        reply.setThreadId(id);
        reply.setAuthorName(request.authorName().trim());
        reply.setBody(request.body());
        reply.setStatus("PENDING");
        reply.setNoticeStatus("NONE");
        replies.save(reply);
        Publish.afterCommit(() -> rabbit.convertAndSend(Channels.MODERATE, new ModerateCommand(
                reply.getId(), reply.getThreadId(), "REPLY", reply.getAuthorName(), reply.getBody())));
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @GetMapping("/threads/{id}")
    public ThreadEntity thread(@PathVariable String id) {
        return threads.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found"));
    }

    @GetMapping("/replies/{id}")
    public ReplyEntity reply(@PathVariable String id) {
        return replies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reply not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateBoardRequest(String name) {
    }

    public record CreateThreadRequest(String authorName, String title, String body) {
    }

    public record ReplyRequest(String authorName, String body) {
    }
}
