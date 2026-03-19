package examples

enum Tier:
  case Plus, Vip

enum Reward:
  case Bonus(points: Int)
  case Grade(mark: Char)

enum Palette:
  case Named(label: String, intensity: Int)
  case Rgb(red: Int, green: Int)

case class DualBox[A, B](right: B, left: A)

case class Person(age: Option[Long], bonus: Long):
  def normalizedBonus: Long =
    if bonus > 0L then bonus + 1L
    else bonus

  def isAtLeast(threshold: Long): Boolean =
    bonus >= threshold

  def adjustedBonus(threshold: Long): Long =
    if bonus + 1L > threshold then bonus + 1L
    else threshold

case class Envelope[A, B](box: DualBox[A, B], fee: Long)

object CurrentSupportedExample:
  def greeting: String =
    "hello"

  def adjustedScore(input: Envelope[Person, Tier], fallbackAge: Long): (Long, Tier) =
    val age =
      input.box.left.age match
        case Some(value) => value
        case None => fallbackAge

    val tierBonus =
      input.box.right match
        case Tier.Vip => 10L
        case Tier.Plus => 5L

    val withBonus = age + input.box.left.normalizedBonus + tierBonus

    val finalScore =
      if input.fee > 0L then withBonus - input.fee
      else withBonus

    val finalTier =
      if finalScore > 20L then Tier.Vip
      else input.box.right

    (finalScore, finalTier)

  def normalizedHistory(input: Option[List[Long]]): List[Long] =
    input match
      case Some(values) => values
      case None => Nil

  def projectTier(result: (Long, Tier)): Tier =
    result match
      case (_, tier) => tier

  def rewardCode(reward: Reward): Char =
    reward match
      case Reward.Bonus(_) => 'B'
      case Reward.Grade(mark) => mark

  def defaultThresholds: List[Int] =
    List(10, 20)

  def thresholdGroups: List[List[Int]] =
    List(List(10, 20), List(30))

  def keepsReward(person: Person, reward: Reward): Reward =
    if person.adjustedBonus(10L) > 10L then reward
    else Reward.Bonus(0)

  def sumPair(pair: (Int, Int)): Int =
    val (left, right) = pair
    left + right

  def firstOfTriple(triple: (Int, String, Boolean)): Int =
    triple match
      case (first, _, _) => first

  def paletteIntensity(palette: Palette): Int =
    palette match
      case Palette.Named(_, intensity) => intensity
      case Palette.Rgb(red, green) => red + green
