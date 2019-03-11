package eu.eyan.game


abstract class Game(state: GameState) {
  def possibleSteps: Seq[GameStep]
  def next(step: GameStep): GameState
//  def nextStates = (possibleSteps map next).toSetlé
}

trait GameState {
  def value(player: GamePlayer):Int
}
trait GameStep
trait GamePlayer