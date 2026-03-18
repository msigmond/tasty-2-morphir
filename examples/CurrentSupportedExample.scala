package examples

case class Box[A](value: A)

case class Person(age: Option[Int], bonus: Int, tier: String)

case class Envelope[A](box: Box[A], fee: Int)

object CurrentSupportedExample:
  def adjustedScore(input: Envelope[Person], fallbackAge: Int): (Int, String) =
    val age =
      input.box.value.age match
        case Some(value) => value
        case None => fallbackAge

    val tierBonus =
      input.box.value.tier match
        case "vip" => 10
        case "plus" => 5
        case _ => 0

    val withBonus = age + input.box.value.bonus + tierBonus

    val finalScore =
      if input.fee > 0 then withBonus - input.fee
      else withBonus

    (finalScore, input.box.value.tier)
