package com.threadhall.board;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadRepository extends JpaRepository<ThreadEntity, String> {
}
