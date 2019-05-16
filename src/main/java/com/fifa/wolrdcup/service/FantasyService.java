package com.fifa.wolrdcup.service;

import com.fifa.wolrdcup.exception.InvalidLeagueIdException;
import com.fifa.wolrdcup.model.*;
import com.fifa.wolrdcup.model.custom.PointsValue;
import com.fifa.wolrdcup.model.league.FantasyLeague;
import com.fifa.wolrdcup.model.league.League;
import com.fifa.wolrdcup.model.league.RegularLeague;
import com.fifa.wolrdcup.model.players.Goalkeaper;
import com.fifa.wolrdcup.model.players.Player;
import com.fifa.wolrdcup.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FantasyService {

    private static Logger logger = LoggerFactory.getLogger(FantasyService.class);

    private final LeagueRepository leagueRepository;

    private final TeamRepository teamRepository;

    private final FantasyLineupRepository fantasyLineupRepository;

    private final LineupRepository lineupRepository;

    private final PlayerRepository playerRepository;

    private final PlayerPointsRepository playerPointsRepository;

    private final MatchRepository matchRepository;

    public FantasyService(PlayerPointsRepository playerPointsRepository,
                          LeagueRepository leagueRepository, TeamRepository teamRepository,
                          FantasyLineupRepository fantasyLineupRepository,
                          LineupRepository lineupRepository, PlayerRepository playerRepository,
                          MatchRepository matchRepository) {
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.fantasyLineupRepository = fantasyLineupRepository;
        this.lineupRepository = lineupRepository;
        this.playerRepository = playerRepository;
        this.playerPointsRepository = playerPointsRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void process(Long leagueId) {
        Optional<League> leagueOptional = leagueRepository.findById(leagueId);

        if(!leagueOptional.isPresent() || !(leagueOptional.get() instanceof RegularLeague)) {
            throw new InvalidLeagueIdException();
        }

        RegularLeague league = (RegularLeague) leagueOptional.get();

        for(Round round : league.getRounds()) {
            for(Match match : round.getMatches()) {
                processMatch(match);
            }
        }
    }

    @Transactional
    public void processMatch(Match match) {
        if(match.getScore1() == null || match.getPlayerPoints().size() > 0) {
            return;
        }

        Map<Long, PointsValue> pointsMap = calculatePointsForMatch(match);

        for(Long playerId : pointsMap.keySet()) {
            PointsValue pointsValue = pointsMap.get(playerId);

            PlayerPoints playerPoints = new PlayerPoints();
            playerPoints.setMatch(match);
            playerPoints.setPlayer(pointsValue.getPlayer());
            playerPoints.setPoints(pointsValue.getTotalPoints());

            playerPointsRepository.save(playerPoints);
        }
    }

    private Map<Long, PointsValue> calculatePointsForMatch(Match match) {
        Map<Long, PointsValue> pointsMap = new HashMap<>();

        for(Goal goal : match.getGoals()) {
            pointsMap.putIfAbsent(goal.getPlayer().getId(), new PointsValue(goal.getPlayer()));

            if(!goal.getOwnGoal()) {
                pointsMap.get(goal.getPlayer().getId()).addGoal();
            } else {
                pointsMap.get(goal.getPlayer().getId()).addOwnGoal();
            }

            if(goal.getAssist() != null) {
                pointsMap.putIfAbsent(goal.getAssist().getId(), new PointsValue(goal.getAssist()));

                pointsMap.get(goal.getAssist().getId()).addAssist();
            }

            if(goal.getScore1() == 0){
                pointsMap.putIfAbsent(goal.getPlayer().getId(), new PointsValue(goal.getPlayer()));

                pointsMap.get(goal.getPlayer().getId()).addCleanSheet();
            }

            if(goal.getScore2() == 0){
                pointsMap.putIfAbsent(goal.getPlayer().getId(), new PointsValue(goal.getPlayer()));

                pointsMap.get(goal.getPlayer().getId()).addCleanSheet();
            }
        }

        for(Card card : match.getCards()) {
            pointsMap.putIfAbsent(card.getPlayer().getId(), new PointsValue(card.getPlayer()));

            if(card.getCardType() == Card.CardType.RED) {
                pointsMap.get(card.getPlayer().getId()).addRedCard();
            } else if(card.getCardType() == Card.CardType.YELLOW) {
                pointsMap.get(card.getPlayer().getId()).addYellowCard();
            }
        }

        for(MissedPenalty missedPenalty : match.getMissedPenalties()){

            if(missedPenalty.getSavedBy() != null) {
                pointsMap.putIfAbsent(missedPenalty.getSavedBy().getId(), new PointsValue(missedPenalty.getSavedBy()));

                pointsMap.get(missedPenalty.getSavedBy().getId()).addSavedPenalty();
            }

            if(missedPenalty.getPlayer() != null){
                pointsMap.putIfAbsent(missedPenalty.getPlayer().getId(), new PointsValue(missedPenalty.getPlayer()));

                pointsMap.get(missedPenalty.getPlayer().getId()).addMissedPenalty();
            }
        }



        Map<Long, Integer> minutesPlayed = getPlayerMinutes(match.getLineup1(), pointsMap, match);
        minutesPlayed.putAll(getPlayerMinutes(match.getLineup2(), pointsMap, match));

        for(Long playerId: minutesPlayed.keySet()) {
            pointsMap.get(playerId).addMinutesPlayed(minutesPlayed.get(playerId));
        }

        return pointsMap;
    }

    private Map<Long, Integer> getPlayerMinutes(Lineup lineup, Map<Long, PointsValue> pointsMap, Match match) {
        Map<Long, Integer> result = new HashMap<>();

        if(lineup == null) {
            return result;
        }

        for(Player player: lineup.getStartingPlayers()) {
            pointsMap.putIfAbsent(player.getId(), new PointsValue(player));

            result.put(player.getId(), 90);
        }

        for(Substitution substitution: lineup.getSubstitutionChanges()) {
            if(substitution.getPlayer() != null) {
                pointsMap.putIfAbsent(substitution.getPlayer().getId(), new PointsValue(substitution.getPlayer()));

                result.put(substitution.getPlayer().getId(), 90 - substitution.getMinute());
            }
        }

        for (Substitution substitution : lineup.getSubstitutionChanges()) {
            Integer minutesPlayed = result.get(substitution.getSubstitutePlayer().getId());

            if(minutesPlayed == null) {
                match.setReviewRequired(true);

                matchRepository.save(match);

                logger.error(
                        "Error while processing minutes for player {}", substitution.getSubstitutePlayer().getId());
                continue;
            }

            result.put(substitution.getSubstitutePlayer().getId(), minutesPlayed - (90 - substitution.getMinute()));
        }

        return result;
    }

    @Transactional
    public void seedFantasyPlayerLeague(Long leagueId) {
        String leagueName = "Fantasy Premijer Liga";

        if(leagueRepository.findByName(leagueName).isPresent()) {
            return;
        }

        FantasyLeague fantasyLeague = new FantasyLeague();
        fantasyLeague.setName(leagueName);

        leagueRepository.findById(leagueId).ifPresent((league -> {
            fantasyLeague.setRegularLeague((RegularLeague) league);
        }));

        leagueRepository.save(fantasyLeague);

        Team fantasyTeam = new Team();
        fantasyTeam.setCode("VUCKO");
        fantasyTeam.setName("Vucko");

        teamRepository.save(fantasyTeam);

        fantasyTeam.getLeagues().add(fantasyLeague);

        teamRepository.save(fantasyTeam);

        for(long playerId = 1L; playerId <= 15; playerId++) {
            Optional<Player> optionalPlayer = playerRepository.findById(playerId);

            if(optionalPlayer.isPresent()) {
                Player player = optionalPlayer.get();

                player.getTeams().add(fantasyTeam);

                playerRepository.save(player);
            }
        }

        FantasyLineup fantasyLineup = new FantasyLineup();
        fantasyLineup.setLeague(fantasyLeague);
        fantasyLineup.setTeam(fantasyTeam);
        fantasyLineup.setRound(fantasyLeague.getRegularLeague().getRounds().get(0));

        Lineup lineup = new Lineup();
        lineup.setFormation(Lineup.Formation.F_4_3_3);

        lineupRepository.save(lineup);

        fantasyLineup.setLineup(lineup);

        fantasyLineupRepository.save(fantasyLineup);
    }
}
