import java.sql.DriverManager
import java.sql.Connection
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer

object DbTail {

  def main(args: Array[String]): Unit = Config(args) map { implicit config =>
    
    val connection = getConnection

    val initialQuery = getInitialQuery
    val statement = connection.createStatement
    val resultSet = statement.executeQuery(initialQuery)
    
    val rows = getRows(resultSet)
    
    rows.foreach(printRow(_))
    
    var lastId = rows.lastOption.flatMap(_.find(_._1 == "id").map(_._2)).getOrElse("0")
    
    while(true) {
      Thread.sleep(500)
      
      val statement = connection.createStatement
      val resultSet = statement.executeQuery(getQuery(lastId))
      val rows = getRows(resultSet)
      rows.foreach(printRow)
      if(rows.nonEmpty) {
        lastId = rows.lastOption.flatMap(_.find(_._1 == "id").map(_._2)).get
      }
    }
    
  }
  
  def getInitialQuery(implicit config: Config): String = 
    config.query + " ORDER BY id ASC limit 10"

  def getQuery(previousHighestPk: String)(implicit config: Config): String = { 
    val clause = if(config.query.toLowerCase contains "where") " AND " else " WHERE "
    config.query + clause + "id > " + previousHighestPk + " ORDER BY id ASC"
  }
  
  def getConnection(implicit config: Config): Connection = {
    import config._
    Class.forName("com.mysql.jdbc.Driver")
    DriverManager.getConnection(s"jdbc:mysql://$host:$port/$database", username, password)
  }
  
  def getRows(resultSet: ResultSet): Seq[Seq[(String, String)]] = {
    val columnNames = getColumnNames(resultSet)
    val rows = ListBuffer[Seq[(String, String)]]()
    while(resultSet.next()) {
      rows += columnNames.map(cn => cn -> Option(resultSet.getString(cn)).getOrElse("<null>"))
    }
    rows.toList
  }
  
  def getColumnNames(resultSet: ResultSet): Seq[String] = {
    val metadata = resultSet.getMetaData
    val count = metadata.getColumnCount
    for(i <- 1 to count) yield {
      metadata.getColumnName(i)
    }
  }
  
  def printRow(columns: Seq[(String, String)]) = {
    val longestName = columns.map(_._1.size).max
    
    columns.map { case (name, value) => 
      val left = name.padTo(longestName + 1, " ").mkString + ": " 
      println(left + chopchop(value, left.size))
    }
    
    println()
  }
  
  // Make a value no more than 200 px wide, max 30 lines long and pad new lines 
  def chopchop(value: String, pad: Int) = {
    val lines = value.split("\n")
    
    val linesShortened = lines.flatMap { line =>
      if(line.size > 80) line.grouped(80) else Seq(line)  
    }
    
    val padded = (linesShortened.head +: linesShortened.tail.map(" " * pad + _))
    
    (if(padded.size > 30) padded.take(20) else padded) mkString "\n"
  }
}

case class Config(
  host: String = "localhost",
  port: Int = 3306,
  database: String = null,
  username: String = null,
  password: String = null,
  queryParts: List[String] = Nil) {
  def addQueryPart(part: String) = copy(queryParts = queryParts :+ part)
  def query = queryParts mkString " "
}

object Config {
  val parser = new scopt.OptionParser[Config]("dbtail") {
    head("dbtail", "0.1")
    opt[String]('h', "host") action((v, c) => c.copy(host = v))
    opt[Int]('P', "port") action((v, c) => c.copy(port = v))
    opt[String]('d', "database") required() action((v, c) => c.copy(database = v))
    opt[String]('u', "username") required() action((v, c) => c.copy(username = v))
    opt[String]('p', "password") required() action((v, c) => c.copy(password = v))
    arg[String]("query") unbounded() action((v, c) => c.addQueryPart(v))
  }

  def apply(args: Array[String]): Option[Config] = parser.parse(args, Config())

}