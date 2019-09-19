package com.atguigu.watermark

import com.atguigu.sensortest.SensorReading
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time

object WatermarkTest1 {

  def main(args: Array[String]): Unit = {
    //创建执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //设置并行度
    env.setParallelism(1)

    //设置时间特性
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //获取数据
    val readDataStream: DataStream[String] = env.socketTextStream("hadoop102", 7777)
    //Transform转换数据格式
    val dataStream: DataStream[SensorReading] = readDataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })

    //TODO watermark
    //    dataStream.assignAscendingTimestamps(_.timestamp * 1000L)

    val watermarkDataStream: DataStream[(String, Double)] = dataStream.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
        override def extractTimestamp(element: SensorReading): Long = {
          element.timestamp * 1000
        }
      }
    )
      .map(data => (data.id, data.temperature))
      .keyBy(_._1)
      .timeWindow(Time.seconds(15), Time.seconds(5))
      .reduce((result, data) => (result._1, result._2.min(data._2)))


    watermarkDataStream.print()

//    dataStream.assignTimestampsAndWatermarks(new MyAssigner())
    //    dataStream.assignAscendingTimestamps(new MyAssigner2())
    env.execute()
  }

}

class MyAssigner() extends AssignerWithPeriodicWatermarks[SensorReading] {

  //定义固定延迟3秒
  val bound: Long = 3000L

  //定义当前收到的最大时间戳
  var maxTs: Long = Long.MinValue

  override def getCurrentWatermark: Watermark = {

    new Watermark(maxTs - bound)
  }

  override def extractTimestamp(element: SensorReading, previousElementTimestamp: Long): Long = {
    maxTs = maxTs.max(element.timestamp * 1000L)
    element.timestamp * 1000L
  }
}


class MyAssigner2() extends AssignerWithPunctuatedWatermarks[SensorReading] {
  val bound: Long = 1000L

  override def checkAndGetNextWatermark(lastElement: SensorReading, extractedTimestamp: Long): Watermark = {
    if (lastElement.id == "sensor_1") {
      new Watermark(extractedTimestamp - bound)
    } else {
      null
    }
  }

  override def extractTimestamp(element: SensorReading, previousElementTimestamp: Long): Long = {
    element.timestamp * 1000L
  }
}
