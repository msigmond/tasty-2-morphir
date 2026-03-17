package examples

case class Box[A](value: A)

case class Person(age: Option[Int], bonus: Int)

case class Envelope[A](box: Box[A], fee: Int)

object CurrentSupportedExample:
  def adjustedScore(input: Envelope[Person], fallbackAge: Int): Int =
    val age =
      input.box.value.age match
        case Some(value) => value
        case None => fallbackAge

    val withBonus = age + input.box.value.bonus

    if input.fee > 0 then withBonus - input.fee
    else withBonus
