package com.atguigu.sinktest

import com.atguigu.sensortest.SensorReading
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}

object RedisSinkTest {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    val dataStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    //Transform操作
    val tranDataStream: DataStream[SensorReading] = dataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })
    //Redis
    val conf: FlinkJedisPoolConfig = new FlinkJedisPoolConfig.Builder().setHost("hadoop102").setPort(6379).build()

    //sink
    tranDataStream.addSink(new RedisSink(conf, new MyRedisMapper()))

    env.execute()
  }
}

class MyRedisMapper() extends RedisMapper[SensorReading] {

  //定义保存数据到Redis的命令
  override def getCommandDescription: RedisCommandDescription = {
    //把传感器id和温度值保存成哈希表 Hset key field value
    new RedisCommandDescription(RedisCommand.HSET, "sensor_temperature")
  }

  //定义保存到Redis的key
  override def getKeyFromData(t: SensorReading): String = t.id

  //定义保存到Redis的value
  override def getValueFromData(t: SensorReading): String = t.temperature.toString
}
