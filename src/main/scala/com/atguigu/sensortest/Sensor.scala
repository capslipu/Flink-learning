package com.atguigu.sensortest

import org.apache.flink.streaming.api.scala._

object Sensor {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream2: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")



    val stream1: DataStream[SensorReading] = env.fromCollection(List(
      SensorReading("sensor_1", 1547718199, 35.80018327300259),
      SensorReading("sensor_6", 1547718201, 15.402984393403084),
      SensorReading("sensor_7", 1547718202, 6.720945201171228),
      SensorReading("sensor_10", 1547718205, 38.101067604893444)
    ))
    stream2.print("stream1")

    env.execute()
  }
}

case class SensorReading(id: String, timestamp: Long, temperature: Double)
