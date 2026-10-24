package com.threadhall.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<DecisionEntity, String> {
}
