package com.motomutterers.boardgames.rooms.model.Room;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/*
  RoomUser {
    uuid id PK
    uuid room_id FK
    uuid user_id FK nullable
    string display_name
    boolean is_anonymous
    string role
    timestamp joined_at
  }
*/

@Entity
@Table(name = "rooms_users")
public class RoomUser {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "room_user_roles")
    private RoomUserRoles role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomUserStatus status;

    // Turn/seat order within the room. Set on creation (admin, invite, anonymous)
    // and rewritten when the admin reorders players; the game uses it for the
    // first-round leader and round rotation.
    @Column(name = "playing_position")
    private int playingPosition;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public RoomUser(){}

    // A joined player (admin on room creation, or an accepted invitee): ACTIVE.
    public RoomUser(User user, Room room, RoomUserRoles role, int playingPosition){
      this.user = user;
      this.room = room;
      this.role = role;
      this.displayName = user.getUsername();
      this.status = RoomUserStatus.ACTIVE;
      this.playingPosition = playingPosition;
    }

    // An invited real player: the RoomUser exists up front as PENDING_INVITE and
    // becomes ACTIVE when they accept.
    public RoomUser(User user, Room room, RoomUserStatus status, int playingPosition){
      this.user = user;
      this.room = room;
      this.role = RoomUserRoles.PLAYER;
      this.displayName = user.getUsername();
      this.status = status;
      this.playingPosition = playingPosition;
    }

    // An anonymous placeholder: always ACTIVE (no login, added directly by admin).
    public RoomUser(String displayName, Room room, int playingPosition){
      this.displayName = displayName;
      this.room = room;
      this.role = RoomUserRoles.ANONYMOUS;
      this.status = RoomUserStatus.ACTIVE;
      this.playingPosition = playingPosition;
    }

    public UUID getId(){
      return id;
    }

    public Room getRoom(){
      return room;
    }

    public User getUser(){
      return user;
    }

    /**
     * The name to show for this player. A real account's name follows the account,
     * so a username change is reflected everywhere at once — including completed
     * matches, whose scoreboards should link to the profile that exists *now*.
     * Only an anonymous placeholder owns its name, since it has no account to
     * follow. The stored column is therefore just the anonymous label.
     */
    public String getDisplayName(){
      return user != null ? user.getUsername() : displayName;
    }

    public RoomUserRoles getRole(){
      return role;
    }

    public RoomUserStatus getStatus(){
      return status;
    }

    public int getPlayingPosition(){
      return playingPosition;
    }

    public LocalDateTime getJoinedAt(){
      return joinedAt;
    }

    public void setRoom(Room room){
        this.room = room;
    }

    public void setUser(User user){
        this.user = user;
    }

    public void setDisplayName(String displayName){
        this.displayName = displayName;
    }

    public void setRole(RoomUserRoles role) {
        this.role = role;
    }

    public void setStatus(RoomUserStatus status){
        this.status = status;
    }

    public void setPlayingPosition(int playingPosition){
        this.playingPosition = playingPosition;
    }

    public Team getTeam(){
        return team;
    }

    public void setTeam(Team team){
        this.team = team;
    }

    public void setJoinedAt(LocalDateTime joinedAt){
        this.joinedAt = joinedAt;
    }
}
