package com.atguigu.sinktest

import java.util

import com.atguigu.sensortest.SensorReading
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink
import org.apache.http.HttpHost
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests

object EsSinkTest {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    val dataStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    //Transform操作
    val tranDataStream: DataStream[SensorReading] = dataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)

    })
    //sink
    val httpHosts = new util.ArrayList[HttpHost]()
    httpHosts.add(new HttpHost("hadoop102", 9200))

    //创建一个esSink的builder
    val esSinkBuilder = new ElasticsearchSink.Builder[SensorReading](
      httpHosts,
      new ElasticsearchSinkFunction[SensorReading] {
        override def process(t: SensorReading, runtimeContext: RuntimeContext, requestIndexer: RequestIndexer)
        : Unit = {
          println("saving data: " + t)

          //包装成一个Map或者JSONObject
          val map = new util.HashMap[String, String]()

          map.put("sensor_id", t.id)
          map.put("temperature", t.temperature.toString)
          map.put("timestamp", t.timestamp.toString)

          //创建index request,准备发送数据
          val indexRequest: IndexRequest = Requests.indexRequest().index("sensor").`type`("readingdata").source(map)

          //利用index发送请求，写入数据
          requestIndexer.add(indexRequest)
          println("data saved.")
        }
      }
    )

    //sink
    tranDataStream.addSink(esSinkBuilder.build())
    env.execute()
  }
}
