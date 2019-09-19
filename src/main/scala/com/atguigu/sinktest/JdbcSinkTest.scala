package com.atguigu.sinktest

import java.sql.{Connection, DriverManager, PreparedStatement}

import com.atguigu.sensortest.SensorReading
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

object JdbcSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val dataStream: DataStream[String] = env.readTextFile("D:\\IdeaProjects\\flink-0408\\input\\sensor")

    //Transform
    val tranDataStream: DataStream[SensorReading] = dataStream.map(data => {
      val dataArray: Array[String] = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble)
    })


    tranDataStream.addSink(new MyJdbcSink())

    env.execute()
  }
}

class MyJdbcSink() extends RichSinkFunction[SensorReading] {

  //定于sql连接、预编译器
  var conn: Connection = _
  var insertStmt: PreparedStatement = _
  var updateStmt: PreparedStatement = _

  //初始化，创建连接和预编译语句
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    conn = DriverManager.getConnection("jdbc:mysql://hadoop102:3306/test", "root", "li1030")
    insertStmt = conn.prepareStatement("insert into temperatures (sensor,temp) values(?,?)")
    updateStmt = conn.prepareStatement("update temperatures set temp = ? where sensor = ?")
  }

  //调用连接，执行sql
  override def invoke(value: SensorReading, context: SinkFunction.Context[_]): Unit = {
    //执行更新语句
    updateStmt.setDouble(1, value.temperature)
    updateStmt.setString(2, value.id)

    updateStmt.execute()

    //如果update没有查到数据，那么执行插入语句
    if (updateStmt.getUpdateCount == 0) {
      insertStmt.setString(1, value.id)
      insertStmt.setDouble(2, value.temperature)

      insertStmt.execute()
    }
  }

  override def close(): Unit = {
    updateStmt.close()
    insertStmt.close()
    conn.close()
  }
}
