package com.atguigu.window

import com.atguigu.sensortest.SensorReading
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object TumblingWindowTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    //读取数据
    //    val readDataStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    val readDataStream: DataStream[String] = env.socketTextStream("hadoop102", 7777)
    val dataStream: DataStream[SensorReading] = readDataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })

    //TODO 使用滚动窗口操作
    val minTempPerWindow: DataStream[(String, Double)] = dataStream.map(r => (r.id, r.temperature))
      .keyBy(_._1)
      .timeWindow(Time.seconds(15)) //定义窗口，后面才是窗口的具体操作
      .reduce((result, data) => (result._1, result._2.min(data._2)))


    minTempPerWindow.print()


    env.execute()
  }
}
