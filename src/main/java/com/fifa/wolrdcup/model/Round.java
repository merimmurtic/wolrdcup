package com.fifa.wolrdcup.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL)
    @JsonView(RoundMatchesView.class)
    private List<Match> matches = new ArrayList<>();

    @ManyToOne
    @JsonView(RoundLeagueView.class)
    private League league;

    public Round(){}

    public Round(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public League getLeague() {
        return league;
    }

    public void setLeague(League league) {
        this.league = league;
    }

    public interface RoundMatchesView {}

    public interface RoundLeagueView {}

    public interface DetailedView extends RoundMatchesView, RoundLeagueView {}
}
