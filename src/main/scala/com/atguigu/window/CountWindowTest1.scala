package com.atguigu.window

import com.atguigu.sensortest.SensorReading
import org.apache.flink.streaming.api.scala._

object CountWindowTest1 {
  def main(args: Array[String]): Unit = {
    //创建执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //设置并行度
    env.setParallelism(1)
    //获取数据
    val readDataStream: DataStream[String] = env.socketTextStream("hadoop102", 7777)
    //Transform转换数据格式
    val dataStream: DataStream[SensorReading] = readDataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })

    //TODO countwindow
    val countDataStream: DataStream[(String, Double)] = dataStream.map(data => (data.id, data.temperature))
      .keyBy(_._1)
      .countWindow(5)
      .reduce((result, data) => (result._1, result._2.max(data._2)))


    countDataStream.print()

    env.execute()
  }
}
