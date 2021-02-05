import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import java.util.concurrent
import java.util.concurrent.atomic._
import Utilities._

object AvgTweetLength {
  def main(args: Array[String]): Unit = {
    // Configure Twitter credentials using twitter.txt
    setupTwitter()
    val ssc = new StreamingContext("local[*]", "AvgTweetLength", Seconds(1))
    setupLogging()

    val tweets = TwitterUtils.createStream(ssc, None)
    val statuses = tweets.map(status => status.getText())
    val lengths = statuses.map(status => status.length())
    //Atomic vars let you access long vars in a safe thread
    var totalTweets = new AtomicLong(0)
    var totalChars = new AtomicLong(0)

    lengths.foreachRDD((rdd, time) => {
      var count = rdd.count()
      if(count > 0) { //dont bother wth empty batches
        totalTweets.getAndAdd(count)
        totalChars.getAndAdd(rdd.reduce(_+_))

        println("total Tweets: "+totalTweets.get()+"total Chars :"+totalChars.get()+"Average: "+totalChars.get()/totalTweets.get())
      }

    })

    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}
