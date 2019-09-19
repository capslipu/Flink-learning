package com.atguigu.wc

import org.apache.flink.api.scala.{AggregateDataSet, DataSet, ExecutionEnvironment}

import org.apache.flink.api.scala._

object WordCount {
  def main(args: Array[String]): Unit = {

    //创建执行环境
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    //从文件中读取数据
    val inputPath = "D:\\IdeaProjects\\flink-0408\\input"

    val inputDS: DataSet[String] = env.readTextFile(inputPath)

    //对单词进行计数
    val wordCount: AggregateDataSet[(String, Int)] = inputDS.flatMap(_.split(" ")).map((_, 1)).groupBy(0).sum(1)

    //打印输出
    wordCount.print()
  }

}
