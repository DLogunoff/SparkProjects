import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

class filter {
  def sparkJob(): Unit = {
    val spark: SparkSession = SparkSession
                                  .builder()
                                  .appName("lab04a")
                                  .getOrCreate()
    val kafkaOptions = Map("kafka.bootstrap.servers" -> "spark-master-1:6667",
                            "subscribe" -> spark.conf.get("spark.filter.topic_name"),
                            "startingOffsets" -> spark.conf.get("spark.filter.offset"),
                            "maxOffsetsPerTrigger" -> "30",
                            "minPartitions" -> "5")
    val df = spark.read.format("kafka").options(kafkaOptions).load

    val schema = StructType(Seq(
      StructField("event_type", StringType, true), StructField("category", StringType, true),
      StructField("item_id", StringType, true), StructField("item_price", IntegerType, true),
      StructField("uid", StringType, true), StructField("timestamp", LongType, true)
    ))

    val formattedDf = df
      .select(col("value").cast("string"))
      .withColumn("value", from_json(col("value"), schema))
      .select("value.*")
      .withColumn("date", date_format(from_unixtime(col("timestamp") / 1000), "yyyyMMdd"))
      .withColumn("p_date", col("date"))

    val path = spark.conf.get("spark.filter.output_dir_prefix")

    formattedDf.filter(col("action_type") === "view")
        .write
        .format("json")
        .option("path", s"file://user/danila.logunov/${path}/view")
        .partitionBy("p_date")
        .save

    formattedDf.filter(col("event_type") === "buy")
      .write
      .format("json")
      .option("path", s"file://user/danila.logunov/${path}/buy")
      .partitionBy("p_date")
      .save

    spark.stop()
  }
  sparkJob()
}