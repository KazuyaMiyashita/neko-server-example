import neko.json._

object Main {

  def main(args: Array[String]): Unit = {

    val json = Json.obj(
      "hello" -> Json.str("world!")
    )

    println(Json.format(json))
  }

}
