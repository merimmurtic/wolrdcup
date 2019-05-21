package com.fifa.wolrdcup.service;

import com.fifa.wolrdcup.model.Match;
import com.fifa.wolrdcup.model.Round;
import com.fifa.wolrdcup.model.league.RegularLeague;
import com.fifa.wolrdcup.repository.MatchRepository;
import com.fifa.wolrdcup.repository.RegularLeagueRepository;
import com.fifa.wolrdcup.repository.RoundRepository;
import com.fifa.wolrdcup.repository.TeamRepository;
import com.fifa.wolrdcup.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MultiLeagueService {

    private static Logger logger = LoggerFactory.getLogger(MultiLeagueService.class);

    private final MatchRepository matchRepository;

    private final RoundRepository roundRepository;

    private final RegularLeagueRepository regularLeagueRepository;

    private final TeamRepository teamRepository;

    public MultiLeagueService(
            MatchRepository matchRepository, RoundRepository roundRepository,
            RegularLeagueRepository regularLeagueRepository, TeamRepository teamRepository) {
        this.matchRepository = matchRepository;
        this.roundRepository = roundRepository;
        this.regularLeagueRepository = regularLeagueRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public void calculateRounds(RegularLeague league) {
        List<Round> rounds = new ArrayList<>();

        List<Match> matches = matchRepository.findMultiLeagueMatches(league.getId());

        LocalDateTime roundStart = null;

        int counter = 1;

        Round round = null;

        for (Match match : matches) {
            if(!CommonUtils.checkIfSameWeek(roundStart, match.getDateTime())) {
                if(round != null) {
                    roundRepository.save(round);
                }

                round = new Round();
                round.setStartDate(match.getDateTime());
                round.setName(counter + ".Matchday");
                round.setLeague(league);

                round = roundRepository.save(round);

                rounds.add(round);

                counter += 1;
            } else {
                round.setEndDate(match.getDateTime());
            }

            match.getRounds().add(round);

            roundStart = match.getDateTime();
        }

        if (round != null) {
            roundRepository.save(round);
        }

        logger.info("{} rounds calculated for league {}!", rounds.size(), league.getName());
    }

    @Transactional
    public void seedTop5League(List<Long> leagueIds) {
        List<RegularLeague> regularLeagues = new ArrayList<>();

        regularLeagueRepository.findAllById(leagueIds).forEach(regularLeagues::add);

        if(regularLeagues.size() > 0) {
            RegularLeague firstLeague = regularLeagues.get(0);

            Optional<RegularLeague> top5LeagueOptional = regularLeagueRepository.findByNameAndSeason(
                    "TOP 5 League", firstLeague.getSeason());

            if (!top5LeagueOptional.isPresent()) {
                RegularLeague top5League = new RegularLeague();
                top5League.setName("TOP 5 League");
                top5League.setSeason(firstLeague.getSeason());
                top5League.getGroups().addAll(regularLeagues);

                regularLeagueRepository.save(top5League);

                regularLeagues.forEach(league -> {
                    league.getTeams().forEach(team -> {
                        team.getLeagues().add(top5League);

                        teamRepository.save(team);
                    });
                });

                calculateRounds(top5League);
            } else {
                logger.info("Top 5 League is already created for season {}", firstLeague.getSeason());
            }
        }
    }
}
