package examples

case class DualBox[A, B](right: B, left: A)

case class Person(age: Option[Int], bonus: Int)

case class Envelope[A, B](box: DualBox[A, B], fee: Int)

object CurrentSupportedExample:
  def adjustedScore(input: Envelope[Person, String], fallbackAge: Int): (Int, String) =
    val age =
      input.box.left.age match
        case Some(value) => value
        case None => fallbackAge

    val tierBonus =
      input.box.right match
        case "vip" => 10
        case "plus" => 5
        case _ => 0

    val withBonus = age + input.box.left.bonus + tierBonus

    val finalScore =
      if input.fee > 0 then withBonus - input.fee
      else withBonus

    (finalScore, input.box.right)
