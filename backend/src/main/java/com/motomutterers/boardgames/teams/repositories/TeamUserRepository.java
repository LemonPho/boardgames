package com.motomutterers.boardgames.teams.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.teams.models.TeamUser;

public interface TeamUserRepository extends JpaRepository<TeamUser, UUID> {
    
}
