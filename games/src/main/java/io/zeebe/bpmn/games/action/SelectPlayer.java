package io.zeebe.bpmn.games.action;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class SelectPlayer implements JobHandler {

  private final Logger log;

  public SelectPlayer(Logger log) {
    this.log = log;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) {
    final var variables = activatedJob.getVariablesAsMap();

    final var players = (Map<String, List<String>>) variables.get("players");
    final int round = (int) variables.get("round");


    final var playerNames = new ArrayList<String>(players.keySet());
    final var nextPlayer = playerNames.get(round % playerNames.size());
    log.info("Choose next player {}.", nextPlayer);

    final int turns = (int) variables.get("turns");
    log.info("Player {} has {} turns.", nextPlayer, turns);
    final var turnArray = new int[turns];

    final var hand = players.get(nextPlayer);

    jobClient.newCompleteCommand(activatedJob.getKey())
        .variables(Map.of(
            "round", round + 1,
            "nextPlayer", nextPlayer,
            "turnArray", turnArray,
            "hand", hand))
        .send();

  }
}