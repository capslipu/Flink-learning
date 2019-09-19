package com.atguigu

import com.atguigu.sensortest.SensorReading
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.{KeyedProcessFunction, ProcessFunction}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

object process {
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

    val increStream: DataStream[String] = dataStream.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
        override def extractTimestamp(element: SensorReading): Long = {
          element.timestamp * 1000
        }
      }
    ).keyBy(_.id)
      .process(new TempIncreWarning())
    //      .print()

    val monitorStream: DataStream[(String, Double)] = dataStream.process(new FreezingMonitor)
    monitorStream.print("main")
    monitorStream.getSideOutput(new OutputTag[String]("freezing-warning")).print("side")
    env.execute()


  }

}

// TODO ---------------------
class TempIncreWarning() extends KeyedProcessFunction[String, SensorReading, String] {
  //将上一个温度值保存成状态
  lazy val lastTemp: ValueState[Double] = getRuntimeContext.getState(new ValueStateDescriptor[Double]("lastTemp", Types.of[Double]))

  //将注册的定时器时间戳保存成状态
  lazy val currentTimer: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("currentTimer", Types.of[Long]))

  override def processElement(value: SensorReading, ctx: KeyedProcessFunction[String, SensorReading, String]#Context, out: Collector[String]): Unit = {

    //取出上一次的温度值
    val preTemp: Double = lastTemp.value()

    //取出定时器的时间戳
    val curTimerTs: Long = currentTimer.value()

    //更新lastTemp温度值
    lastTemp.update(value.temperature)

    //假如温度上升，而且没有注册过定时器，注册定时器
    if (value.temperature > preTemp && curTimerTs == 0) {
      val timerTs: Long = ctx.timerService().currentProcessingTime() + 5000L
      ctx.timerService().registerProcessingTimeTimer(timerTs)
      //将定时器时间戳保存进状态
      currentTimer.update(timerTs)
    } else if (value.temperature < preTemp) {
      //如果温度下降，就取消定时器
      ctx.timerService().deleteProcessingTimeTimer(curTimerTs)
      currentTimer.clear()
    }

  }

  //5秒后定时器触发，输出报警
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, SensorReading, String]#OnTimerContext,
                       out: Collector[String]): Unit = {
    out.collect(ctx.getCurrentKey + "传感器温度在5秒内连续上升")
    currentTimer.clear()
  }
}

class FreezingMonitor extends ProcessFunction[SensorReading, (String, Double)] {
  override def processElement(value: SensorReading, ctx: ProcessFunction[SensorReading, (String, Double)]#Context,
                              out: Collector[(String, Double)]): Unit = {
    if (value.temperature < 32.0) {
      ctx.output(new OutputTag[String]("freezing-warning"), "Freezing warning")
    }
    //所有数据都发出到主流
    out.collect(value.id, value.temperature)
  }
}
