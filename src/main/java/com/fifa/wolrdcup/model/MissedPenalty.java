package com.fifa.wolrdcup.model;

import com.fifa.wolrdcup.model.players.Player;

import javax.persistence.*;

@Entity
public class MissedPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Match match;

    @ManyToOne
    private Player player;

    @ManyToOne
    private Player concededBy;

    @ManyToOne
    private Player savedBy;

    private Integer minute;

    public MissedPenalty(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getConcededBy() {
        return concededBy;
    }

    public void setConcededBy(Player concededBy) {
        this.concededBy = concededBy;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Player getSavedBy() {
        return savedBy;
    }

    public void setSavedBy(Player savedBy) {
        this.savedBy = savedBy;
    }
}