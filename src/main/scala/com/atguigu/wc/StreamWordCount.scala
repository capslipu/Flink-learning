package com.atguigu.wc

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.api.scala._

object StreamWordCount {
  def main(args: Array[String]): Unit = {

    //从外部命令中获取参数
    val parms: ParameterTool = ParameterTool.fromArgs(args)

    val host: String = parms.get("host")
    val port: Int = parms.getInt("port")

    //创建执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //接受socket文本流
    val textDStream: DataStream[String] = env.socketTextStream(host, port)

    //处理数据
    val dataStream: DataStream[(String, Int)] = textDStream.flatMap(_.split(" ")).filter(_.nonEmpty).map((_, 1)).keyBy(0).sum(1)

    dataStream.print().setParallelism(1)

    //启动executor，执行任务
    env.execute()
  }
}
