package examples

enum Tier:
  case Plus, Vip

case class DualBox[A, B](right: B, left: A)

case class Person(age: Option[Int], bonus: Int)

case class Envelope[A, B](box: DualBox[A, B], fee: Int)

object CurrentSupportedExample:
  def adjustedScore(input: Envelope[Person, Tier], fallbackAge: Int): (Int, Tier) =
    val age =
      input.box.left.age match
        case Some(value) => value
        case None => fallbackAge

    val tierBonus =
      input.box.right match
        case Tier.Vip => 10
        case Tier.Plus => 5

    val withBonus = age + input.box.left.bonus + tierBonus

    val finalScore =
      if input.fee > 0 then withBonus - input.fee
      else withBonus

    val finalTier =
      if finalScore > 20 then Tier.Vip
      else input.box.right

    (finalScore, finalTier)
