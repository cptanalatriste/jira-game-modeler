package crest.jira.gametheory.priority.game;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EstimatedGame {

  private static Logger logger = Logger.getLogger(EstimatedGame.class.getName());
  private static int FIRST_PLAYER_INDEX = 0;

  private int numberOfPlayers;
  private double[] strategySubset;
  private List<ReleaseTestStrategyProfile> strategyProfiles;

  /**
   * Approximates an infinite game.
   */
  public EstimatedGame(int strategySubsetSize, double maxStrategyValue, int numberOfPlayers) {
    this.numberOfPlayers = numberOfPlayers;
    this.strategySubset = new double[strategySubsetSize];
    loadStrategySubset(strategySubsetSize, maxStrategyValue);

    this.strategyProfiles = new ArrayList<>();
    this.loadStrategyProfiles();
  }

  private void loadStrategySubset(int strategySubsetSize, double maxStrategyValue) {
    double stepSize = maxStrategyValue / strategySubsetSize;
    double strategyValue = 0;
    for (int index = 0; index < strategySubset.length; index += 1) {
      strategySubset[index] = strategyValue;
      strategyValue += stepSize;
    }
  }

  private void loadStrategyProfiles() {
    logger.info("Starting profile calculations for " + numberOfPlayers + " players with "
        + strategySubset.length + " strategies each.");
    loadStrategyProfiles(FIRST_PLAYER_INDEX, new double[numberOfPlayers], this);
  }

  /**
   * Calculates the permutations with repetition of the number of strategies per
   * number of players.
   * 
   */
  public static void loadStrategyProfiles(int playerIndex, double[] playerStrategies,
      EstimatedGame estimatedGame) {
    if (playerIndex >= estimatedGame.getNumberOfPlayers()) {
      estimatedGame.getStrategyProfiles().add(new ReleaseTestStrategyProfile(
          ArrayUtils.toObject(estimatedGame.getStrategySubset()), playerStrategies));
      return;
    } else if (playerIndex < estimatedGame.getNumberOfPlayers()) {
      for (double strategy : estimatedGame.getStrategySubset()) {
        playerStrategies[playerIndex] = strategy;
        loadStrategyProfiles(playerIndex + 1, playerStrategies, estimatedGame);
      }
    }

  }

  public int getNumberOfPlayers() {
    return numberOfPlayers;
  }

  public double[] getStrategySubset() {
    return strategySubset;
  }

  public List<ReleaseTestStrategyProfile> getStrategyProfiles() {
    return strategyProfiles;
  }
}
