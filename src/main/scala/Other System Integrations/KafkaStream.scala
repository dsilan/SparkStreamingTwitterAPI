import Utilities.{apacheLogPattern, setupLogging}
import org.apache.commons.codec.StringDecoder
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
import java.util.regex.Pattern
import java.util.regex.Matcher
import Utilities._
import org.apache.spark.streaming.kafka._
import kafka.serializer.StringDecoder

object KafkaStream {
  def main(args: Array[String]): Unit = {

    val ssc = new StreamingContext("local[*]", "KafkaStreaming", Seconds(1))
    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()
    //hostname:port for Kafka brokers, not Zookeeper
    //brokers form the kafka cluster. Broker is server keeps topics and partitions.
    val kafkaParams = Map("metadata.broker.list" -> "localhost:9092")
    //List of topics you want to listen for from Kafka
    val topics = List("testLogs").toSet
    //Create Kafka Stream whicj will contain(topic, message) pairs. map(_._2) -> gets only messages, contains individual lines of data
    val lines = KafkaUtils.createDirectStream[String,String,StringDecoder,StringDecoder](ssc, kafkaParams,topics).map(_._2)


  }
}
