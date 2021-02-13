import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel

import java.util.regex.Pattern
import java.util.regex.Matcher
import Utilities._
import org.apache.spark.sql.SparkSession

object LogSQL {
  case class Record (url:String, status:Int, agent:String)
  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val conf = new SparkConf().setMaster("local[*]").setAppName("LogSQL").set("spark.sql.warehouse.dir","file:///C:/tmp")
    val ssc = new StreamingContext(conf , Seconds(1))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines = ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)

    // Extract the request field from each log line
    val requests = lines.map(x => {
      val matcher:Matcher = pattern.matcher(x);
        if (matcher.matches()){
          val request = matcher.group(5)
          val requestFields = request.toString().split(' ')
          val url = util.Try(requestFields(1)) getOrElse "[err]"
        (url, matcher.group(6).toInt,matcher.group(9)) //return url, status code, user agent
        } else {
          ("err",0,"err")
        }
      }
    )
    requests.foreachRDD((rdd,time)=> {
      val ss = SparkSession.builder()
        .appName("LogSQL")
    })

    // Kick it off
    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}
