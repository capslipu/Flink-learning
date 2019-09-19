package com.atguigu.sensortest

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011

import scala.collection.immutable
import scala.util.Random

object KafkaSource {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    //从kafka中读取数据
    //创建kafka相关配置

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "hadoop102:9092")
    properties.setProperty("group.id", "consumer-group")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("sensortest", new
        SimpleStringSchema(), properties))

    //  stream.print()

    //TODO 自定义数据源
    val stream2: DataStream[SensorReading] = env.addSource(new SensorSource())

    stream2.print("stream2")
    env.execute()
  }
}

class SensorSource extends SourceFunction[SensorReading] {

  //定义一个flag，表示数据源是否还在正常运行
  var runing: Boolean = true


  override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit = {

    //创建一个随机数发生器
    val random = new Random()

    //随机初始值生成10 个传感器的温度数据，之后再它基础随机波动生成流数据
    var curTemp: immutable.IndexedSeq[(String, Double)] = 1.to(10).map(
      i => ("sensor_" + i, 60 + random.nextGaussian() * 20)
    )
    //无限循环生成流数据，除非被cancel

    while (runing) {
      //更新温度值

      curTemp = curTemp.map(
        t => (t._1, t._2 + random.nextGaussian())
      )

      //获取当前时间戳
      val curTime: Long = System.currentTimeMillis()

      curTemp.foreach(
        t => sourceContext.collect(SensorReading(t._1, curTime, t._2))
      )

      Thread.sleep(500)

    }

  }

  override def cancel(): Unit = runing = false
}
