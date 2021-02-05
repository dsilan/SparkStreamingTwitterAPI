import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import java.util.concurrent.atomic._
import Utilities._

object FindLongestTweetLength {
  def main(args: Array[String]): Unit = {
    // Configure Twitter credentials using twitter.txt
    setupTwitter()
    val ssc = new StreamingContext("local[*]", "FindLongestTweetLength", Seconds(1))
    setupLogging()

    val tweets = TwitterUtils.createStream(ssc, None)
    val statuses = tweets.map(status => status.getText())

    val lengths = statuses.map(status => status.length())

    //Atomic vars let you access long vars in a safe thread
    var totalTweets = new AtomicLong(0)
    var rddLongest = new AtomicInteger(0)
    var totalLongest = new AtomicInteger(0)

    lengths.foreachRDD((rdd,time) => {
      var count = rdd.count()
      if(count > 0){ //don't bother wth empty batches
        totalTweets.getAndAdd(count)

        val repartitionedRDD = rdd.repartition(1).cache() //combine each partitions into a single rdd
        rddLongest.set(repartitionedRDD.max())
        println("Length of Longest Tweet in this RDD = "+ rddLongest.get())

        //total max
        if(rddLongest.get() > totalLongest.get()) {
          totalLongest.set(rddLongest.get())
        }

        if(totalTweets.get()>1000){
          println("Length of Longest Tweet of all RDDs = "+ totalLongest.get())
          System.exit(0)
        }

      }
    })

    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}
