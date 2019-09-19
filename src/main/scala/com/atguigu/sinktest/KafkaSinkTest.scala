package com.atguigu.sinktest

import java.util.Properties

import com.atguigu.sensortest.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer011, FlinkKafkaProducer011}

object KafkaSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
//        val inputStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    val properties = new Properties()

    properties.setProperty("bootstrap.servers", "hadoop102:9092")
    properties.setProperty("group.id", "consumer-group")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")
    val dataStream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("sensortest",
      new SimpleStringSchema, properties))

    //Transform操作
    val tranDataStream: DataStream[String] = dataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble).toString
    })

    //sink
    tranDataStream.addSink(new FlinkKafkaProducer011[String]("hadoop102:9092", "sinktest", new SimpleStringSchema()))
    dataStream.print()

    env.execute()

  }
}
