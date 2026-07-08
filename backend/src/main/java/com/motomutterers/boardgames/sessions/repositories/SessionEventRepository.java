package com.motomutterers.boardgames.sessions.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;

public interface SessionEventRepository extends JpaRepository<SessionEvent, UUID>{}