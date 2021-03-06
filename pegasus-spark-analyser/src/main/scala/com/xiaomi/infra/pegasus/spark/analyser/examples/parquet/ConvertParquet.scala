package com.xiaomi.infra.pegasus.spark.analyser.examples.parquet

import com.xiaomi.infra.pegasus.spark.FDSConfig
import com.xiaomi.infra.pegasus.spark.analyser.{
  ColdBackupConfig,
  ColdBackupLoader,
  PegasusContext
}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

object ConvertParquet {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("convertParquet")
      .master(
        "local[1]"
      ) // this config only for test at local, remove it before deploy in cluster
      .getOrCreate()

    // if data in HDFS, pass HDFSConfig()
    val coldBackupConfig =
      new ColdBackupConfig(new FDSConfig("", "", "", "", ""), "onebox", "temp")

    val pc = new PegasusContext(spark.sparkContext)
    val rdd = pc.pegasusSnapshotRDD(new ColdBackupLoader(coldBackupConfig))

    // please make sure data can be converted valid string value
    val dataFrame = spark.createDataFrame(
      rdd.map(i =>
        Row(new String(i.hashKey), new String(i.sortKey), new String(i.value))
      ),
      Schema.struct
    )

    dataFrame
      .coalesce(1)
      .write
      .format("parquet")
      .mode(SaveMode.Overwrite)
      // in online, need save on hdfs
      .save("/tmp")

    //after convert success(only test)
    val dataFrameResult = spark.read.parquet("/tmp")
    dataFrameResult.show(10)
  }
}
