import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
import java.util.regex.Pattern
import java.util.regex.Matcher
import Utilities._

object LogAlarmer {
  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext("local[*]", "LogAlarmer", Seconds(1))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines = ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)

    // Extract the status field from each log line
    val statuses = lines.map( x => {val matcher:Matcher = pattern.matcher(x);
      if (matcher.matches())
        matcher.group(6)
      else "{error}"
    })
    //map statuses with results to success and failure
    val successFailure = statuses.map(x=>{
      val statusCode = util.Try(x.toInt) getOrElse 0
      if(statusCode>= 200 && statusCode < 300){
        "Success"
      } else if (statusCode>= 500 && statusCode < 600){
        "Failure"
      } else "Other"
    })
    //rdd with 5 min window
    val statusCounts = successFailure.countByValueAndWindow(Seconds(300), Seconds(1))

    statusCounts.foreachRDD((rdd,time)=>{
      var totalSuccess: Long = 0
      var totalFailure:Long = 0

      if(rdd.count>0){
        val elements = rdd.collect()
        for(el <- elements){
          val result = el._1
          val count = el._2
          if(result == "Success"){ totalSuccess += count}
          if(result == "Failure"){ totalFailure += count}
        }
      }
      println("Total success: " + totalSuccess + "Total failure: " + totalFailure )
      //alarm only when its necessary
      if(totalSuccess + totalFailure> 100){
        val ratio: Double = util.Try( //prevent zero divison
            totalFailure.toDouble / totalSuccess.toDouble
        ) getOrElse 1
        if(ratio > 0.5) { //wake someone if there are more errors than successes
          println("Wake someone up! Smth ia wrong!")
        } else {
          println("All systems go.")
        }
      }
    })

    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}
