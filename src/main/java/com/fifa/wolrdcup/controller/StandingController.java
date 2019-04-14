package com.fifa.wolrdcup.controller;

import com.fifa.wolrdcup.model.*;
import com.fifa.wolrdcup.repository.MatchRepository;
import com.fifa.wolrdcup.repository.TeamRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/standings")
public class StandingController {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public StandingController(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public Iterable<StandingValue> getStandings() {
        List<StandingValue> result = new ArrayList<>();

        Iterable<Team> teams = teamRepository.findAll();

        for(Team team : teams){
            StandingValue value = new StandingValue();
            value.setTeamId(team.getId());
            value.setTeamName(team.getName());

            //TODO: These 5 lines should be removed and changed with setTeamStats(team, value)
            //TODO: getGoalsScore, getGoalsConceded, getWon, getDraw and getLose methods should be removed
            value.setGoalsScored(getGoalsScored(team));
            value.setGoalsConceded(getGoalsConceded(team));
            value.setLose(getLose(team));
            value.setWon(getWon(team));
            value.setDraw(getDraw(team));

            result.add(value);
        }

        return result;
    }

    private void setTeamStats(Team team, StandingValue value) {
        long goalsScored = 0L;
        long goalsConceded = 0L;
        long won = 0L;
        long lose = 0L;
        long draw = 0L;

        //TODO: Do implementation here

        value.setGoalsScored(goalsScored);
        value.setGoalsConceded(goalsConceded);
        value.setLose(lose);
        value.setWon(won);
        value.setDraw(draw);
    }

    private Long getGoalsScored(Team team) {
        long goalsScored = 0L;

        Iterable<Match> matches = matchRepository.findByTeam1OrTeam2(team, team);
        for(Match match : matches){

            // TODO: Do not compare team name, two teams with same name can exist.
            // For comparation use team id, do same for other methods
            if(team.getName().equals(match.getTeam1().getName())) {

                goalsScored += match.getScore1();
            }
            if(team.getName().equals(match.getTeam2().getName())){

                goalsScored += match.getScore2();
            }
        }

        return goalsScored;
    }

    private Long getGoalsConceded(Team team) {
        long goalsConceded = 0L;

        Iterable<Match> matches = matchRepository.findByTeam1OrTeam2(team, team);
        for(Match match : matches){

            if(team.getName().equals(match.getTeam1().getName())) {

                goalsConceded += match.getScore2();
            }
            if(team.getName().equals(match.getTeam2().getName())){

                goalsConceded += match.getScore1();
            }
        }
        return goalsConceded;
    }

    private Long getWon(Team team) {
        long won = 0L;

        Iterable<Match> matches = matchRepository.findByTeam1OrTeam2(team, team);

        for(Match match : matches){

            if(team.getName().equals(match.getTeam1().getName())){

                if (match.getScore1() > match.getScore2()){
                    won += 1;
                }
            }else if((team.getName().equals(match.getTeam2().getName()))){

                if (match.getScore1() < match.getScore2()){
                    won += 1;
                }
            }
        }
        return won;
    }

    private Long getLose(Team team) {
        long lose = 0L;

        Iterable<Match> matches = matchRepository.findByTeam1OrTeam2(team, team);

        for(Match match : matches){

            if(team.getName().equals(match.getTeam1().getName())){

                if (match.getScore1() < match.getScore2()){
                    lose += 1;
                }

            }else if((team.getName().equals(match.getTeam2().getName()))){

                if (match.getScore1() > match.getScore2()){
                    lose += 1;
                }
            }
        }
        return lose;
    }

    private Long getDraw(Team team) {
        long draw = 0L;

        Iterable<Match> matches = matchRepository.findByTeam1OrTeam2(team, team);

         for(Match match : matches){

            if(team.getName().equals(match.getTeam1().getName())){

                // TODO: Don't use == for comparation, use equals
                if (match.getScore1() == match.getScore2()){
                    draw += 1;
                }

            }else if((team.getName().equals(match.getTeam2().getName()))){

                if (match.getScore1() == match.getScore2()){
                    draw += 1;
                }
            }
        }
        return draw;
    }
}
