package examples

enum Tier:
  case Plus, Vip

case class DualBox[A, B](right: B, left: A)

case class Person(age: Option[Long], bonus: Long)

case class Envelope[A, B](box: DualBox[A, B], fee: Long)

object CurrentSupportedExample:
  def adjustedScore(input: Envelope[Person, Tier], fallbackAge: Long): (Long, Tier) =
    val age =
      input.box.left.age match
        case Some(value) => value
        case None => fallbackAge

    val tierBonus =
      input.box.right match
        case Tier.Vip => 10L
        case Tier.Plus => 5L

    val withBonus = age + input.box.left.bonus + tierBonus

    val finalScore =
      if input.fee > 0L then withBonus - input.fee
      else withBonus

    val finalTier =
      if finalScore > 20L then Tier.Vip
      else input.box.right

    (finalScore, finalTier)
