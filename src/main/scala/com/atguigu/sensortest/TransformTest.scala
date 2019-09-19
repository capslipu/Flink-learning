package com.atguigu.sensortest

import org.apache.flink.api.common.functions.FilterFunction
import org.apache.flink.streaming.api.scala._

object TransformTest {
  def main(args: Array[String]): Unit = {

    //创建执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    //读入数据
    val inputDataStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    //Transform操作
    val dataStream: DataStream[SensorReading] = inputDataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })

    //1.TODO 聚合操作
    val result: DataStream[SensorReading] = dataStream.keyBy("id")
      //      .sum("temperature")
      .reduce((x, y) => SensorReading(x.id, x.timestamp + 10, y.temperature + 20))
    //    result.print()

    //2.TODO 分流 根据温度是否大于30 度划分  split分流splitStream------->select变为DataStream
    val splitStream: SplitStream[SensorReading] = dataStream.split(sensorData => {
      if (sensorData.temperature > 30) Seq("high") else Seq("low")
    })
    val highStream: DataStream[SensorReading] = splitStream.select("high")
    val lowStream: DataStream[SensorReading] = splitStream.select("low")

    //    highStream.print()

    //3.TODO 合并两条流
    val warningStream: DataStream[(String, Double)] = highStream.map(sensorData => (sensorData.id, sensorData.temperature))

    val connectedStream: ConnectedStreams[(String, Double), SensorReading] = warningStream.connect(lowStream)

    val coMapStream: DataStream[Product] = connectedStream.map(
      //TODO 有两条流需要处理，connect之前为第一条流
      warningData => (warningData._1, warningData._2, "high temperature warning"),
      lowData => (lowData.id, "healthy")
    )

//    coMapStream.print()

    //TODO 函数类
    dataStream.filter(new MyFilter()).print()


    env.execute()
  }
}

class MyFilter() extends FilterFunction[SensorReading] {
  override def filter(t: SensorReading): Boolean = {
    t.id.startsWith("sensor_1")
  }
}
