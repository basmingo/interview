package forex.services.metrics

final case class MetricKey(name: String, labels: Map[String, String]) {
  def rendered: String = {
    val labelsPart =
      if (labels.isEmpty) ""
      else labels.toList.sortBy(_._1).map { case (k, v) => s"""$k="$v"""" }.mkString("{", ",", "}")
    s"$name$labelsPart"
  }
}
