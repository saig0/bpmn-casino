package io.zeebe.bpmn.games.bot;

import io.zeebe.bpmn.games.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RandomBot implements GameBot {

  @Override
  public CompletableFuture<List<Card>> selectCardsToPlay(PlayerTurn playerTurn) {
    return CompletableFuture.completedFuture(selectCards(playerTurn.getHandCards()));
  }

  @Override
  public CompletableFuture<Boolean> nopeThePlayedCard(NopeTurn nopeTurn) {
    final var wantToNope = ThreadLocalRandom.current().nextDouble() > 0.5;

    return CompletableFuture.completedFuture(wantToNope);
  }

  @Override
  public CompletableFuture<List<Card>> alterTheFuture(PlayerTurn playerTurn, List<Card> cards) {

    final var alteredFuture = new ArrayList<>(cards);
    Collections.shuffle(alteredFuture);

    return CompletableFuture.completedFuture(alteredFuture);
  }

  @Override
  public CompletableFuture<String> selectPlayer(String player, PlayersOverview playersOverview) {

    final var players = playersOverview.getPlayers();
    final var index = ThreadLocalRandom.current().nextInt(0, players.size());
    final var otherPlayer = players.get(index).getName();

    return CompletableFuture.completedFuture(otherPlayer);
  }

  @Override
  public CompletableFuture<Card> selectCardToGive(String player, List<Card> handCards) {

    final int randomCardIndex = ThreadLocalRandom.current().nextInt(0, handCards.size());
    final var card = handCards.get(randomCardIndex);

    return CompletableFuture.completedFuture(card);
  }

  @Override
  public CompletableFuture<Integer> selectPositionToInsertExplodingCard(
      PlayerTurn playerTurn, Card card) {

    final int index = ThreadLocalRandom.current().nextInt(0, playerTurn.getDeckSize());

    return CompletableFuture.completedFuture(index);
  }

  private List<Card> selectCards(List<Card> handCards) {

    final boolean playCard = ThreadLocalRandom.current().nextDouble() < 0.25;
    if (!playCard) {
      // pass this turn
      return List.of();
    }

    final Map<CardType, List<Card>> catCards =
        handCards.stream()
            .filter(card -> card.getType().isCatCard())
            .collect(Collectors.groupingBy(Card::getType));

    final var twoSameCatCards =
        catCards.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .map(e -> e.getValue().subList(0, 2))
            .findFirst();

    final var catCard =
        catCards.keySet().stream()
            .filter(card -> card != CardType.FERAL_CAT)
            .findFirst()
            .map(type -> catCards.get(type).get(0));

    final var feralCatCard =
        Optional.ofNullable(catCards.get(CardType.FERAL_CAT)).map(cards -> cards.get(0));

    final var feralAndOtherCat =
        Optional.of(catCards)
            .filter(cards -> cards.size() >= 2)
            .filter(cards -> cards.containsKey(CardType.FERAL_CAT))
            .map(cards -> List.of(feralCatCard.get(), catCard.get()));

    final var actionCards =
        handCards.stream()
            .filter(card -> card.getType() != CardType.NOPE && card.getType() != CardType.DEFUSE)
            .filter(card -> !card.getType().isCatCard())
            .collect(Collectors.toList());

    final var actionCard =
        Optional.of(actionCards)
            .filter(cards -> !cards.isEmpty())
            .map(
                cards -> {
                  final var cardIdx = ThreadLocalRandom.current().nextInt(0, cards.size());
                  return List.of(cards.get(cardIdx));
                });

    return twoSameCatCards.or(() -> feralAndOtherCat).or(() -> actionCard).orElse(List.of());
  }
}
