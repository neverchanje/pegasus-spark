package com.xiaomi.infra.pegasus.analyser.sample.spark

import com.xiaomi.infra.pegasus.analyser.{Config, FDSService, PegasusClient}
import org.apache.commons.logging.LogFactory
import org.apache.spark.{SparkConf, SparkContext, TaskContext}
import org.rocksdb.RocksDB

object CountAllData {

  class countAllData

  private val LOG = LogFactory.getLog(classOf[countAllData])

  def main(args: Array[String]): Unit = {

    val config = new Config("core-site.xml")
    val fdsService = new FDSService(config, "c3srv-browser", "browser_feed_user_channel")
    val partitionCount = fdsService.getPartitionCount

    val conf = new SparkConf()
      .setAppName("pegasus data analyse")
      .setIfMissing("spark.master", "local[1]")
      .set("spark.executor.instances", partitionCount.toString)

    val sc = new SparkContext(conf)
    val list = 0 until partitionCount
    val data = sc.parallelize(list, partitionCount)

    val counters = data.mapPartitions(i => {
      //RocksDB.loadLibrary() must at here
      RocksDB.loadLibrary()
      val result = List[Int]()
      val pegasusClient = new PegasusClient(config, fdsService)
      val pid = TaskContext.getPartitionId()
      val counter = pegasusClient.getDataCount(pid)
      LOG.info("partitionId: " + TaskContext.getPartitionId() + " has completed,the counter update to " + counter)
      result.::(counter).iterator
    }).reduce((a, b) => {
      a + b
    })

    LOG.info("all partition data count:" + counters)
  }
}