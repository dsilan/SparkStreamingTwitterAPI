import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._
import org.apache.log4j.Level
import Utilities._
object SaveTweets {
  def main(args: Array[String]): Unit = {
    setupTwitter()

    val ssc = new StreamingContext("local[*]", "PrintTweets", Seconds(1))
    setupLogging()
    val tweets= TwitterUtils.createStream(ssc, None) //create a Dstream
    val statuses = tweets.map(status => status.getText())
    var totalTweets: Long = 0
    statuses.foreachRDD((rdd,time) => {
      if(rdd.count() > 0){ //dont bother wth empty batches
        val repartitionedRDD = rdd.repartition(1).cache() //combine each partitions into a single rdd (part-000? file for each rdd)
        repartitionedRDD.saveAsTextFile("Tweets_"+time.milliseconds.toString) //save text file
        totalTweets += repartitionedRDD.count()
        println("Tweets counts = "+totalTweets)
        if(totalTweets>1000){
          System.exit(0)
        }
      }
    })

    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()

  }
}
