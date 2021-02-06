import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import java.util.concurrent.atomic._
import Utilities._

object MostPopularHashtags {
  def main(args: Array[String]): Unit = {
    // Configure Twitter credentials using twitter.txt
    setupTwitter()
    val ssc = new StreamingContext("local[*]", "MostPopularHashtags", Seconds(1))
    setupLogging()

    val tweets = TwitterUtils.createStream(ssc, None)
    val statuses = tweets.map(status => status.getText())
    val words = statuses.flatMap(s => s.split(' '))
    //filter by hashtags
    val hashtags = words.filter(w => w.startsWith("#"))
    val hashtagKeyValues = hashtags.map(hashtag => (hashtag,1))
    //sum total count values by keys
    val hashtagsWithCounts = hashtagKeyValues.reduceByKeyAndWindow(_+_,_-_,Seconds(300),Seconds(1))
    val mostPopularHashtagsSorted = hashtagsWithCounts.transform(rdd => rdd.sortBy(x => x._2, false))

    mostPopularHashtagsSorted.print //prints top result

    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}
