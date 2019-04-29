package com.fifa.wolrdcup.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fifa.wolrdcup.exception.InvalidPlayerIdException;
import com.fifa.wolrdcup.exception.InvalidTeamIdException;
import com.fifa.wolrdcup.model.players.Player;
import com.fifa.wolrdcup.model.views.DefaultView;
import com.fifa.wolrdcup.model.Team;
import com.fifa.wolrdcup.repository.PlayerRepository;
import com.fifa.wolrdcup.repository.TeamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamController(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @GetMapping
    @JsonView(DefaultView.class)
    public Iterable<Team> getTeams() throws Exception{
        return teamRepository.findAll();
    }

    @GetMapping("/{teamId}")
    @JsonView(value = {Team.DetailedView.class})
    public ResponseEntity<Team> getTeam(@PathVariable("teamId") Long teamId) throws Exception {
        return ResponseEntity.of(teamRepository.findById(teamId));
    }

    @GetMapping("/search/{query}")
    @JsonView(DefaultView.class)
    public List<Team> searchTeams(@PathVariable("query") String query) throws Exception {
        return teamRepository.findByNameContaining(query);
    }

    @PostMapping
    @JsonView(Team.DetailedView.class)
    public ResponseEntity<Team> createTeam(@RequestBody Team team, UriComponentsBuilder builder) {
        // Make sure id is null to avoid update of existing league
        team.setId(null);

        if(team.getCode() == null) {
            team.setCode(team.getName());
        }

        try {
            team = teamRepository.save(team);

            return ResponseEntity.created(
                    builder.path("/teams/{id}").buildAndExpand(team.getId()).toUri()
            ).body(team);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team with provided code already exist!");
        } catch (ConstraintViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getConstraintViolations().toString());
        }
    }

    @PostMapping("/{teamId}/add-player")
    public void referenceTeam(
            @PathVariable("teamId") Long teamId,
            @RequestBody Player player) {
        if(player.getId() != null){
            Optional<Player> existingPlayerOptional = playerRepository.findById(player.getId());

            if(!existingPlayerOptional.isPresent()) {
                throw new InvalidPlayerIdException();
            }

            Optional<Team> existingTeamOptional = teamRepository.findById(teamId);

            if(!existingTeamOptional.isPresent()) {
                throw new InvalidTeamIdException();
            }

            Team team = existingTeamOptional.get();

            player = existingPlayerOptional.get();

            if(!player.getTeams().contains(team)) {
                player.getTeams().add(team);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Player is already referenced with provided team!");
            }

            playerRepository.save(player);
        } else {
            throw new InvalidPlayerIdException();
        }
    }
}

