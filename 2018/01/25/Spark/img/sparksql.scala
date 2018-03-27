
import org.apache.spark.streaming.mqtt.MQTTUtils

import org.apache.spark._

import org.apache.spark.streaming._


/** Computes an approximation to pi */
object Sparksql {

  def sql1(): Unit ={
    val hdfsFilePath1 = "hdfs:///user/hadoop/input/device/"
    val hdfsFilePath2 = "hdfs:///user/hadoop/input/dvalue/"
  // val hdfsFilePath1 = "file:///home/hadoop/Desktop/test1.txt"
   // val hdfsFilePath2 = "file:///home/hadoop/Desktop/test2.txt"
    val conf = new SparkConf()
    conf.setAppName("sql_1")
    val sc = new SparkContext(conf)
    val Device=sc.textFile(hdfsFilePath1, 3)
    val tblRdd1 =Device.map(line => line.split(","))

    val firstSelRdd1= tblRdd1.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DeviceRdd = firstSelRdd1.map(selData => {
      val id = selData._1.toInt
      val type_ = selData._2
      val location = selData._3
      (id, type_, location)
    })


    val Device_key= DeviceRdd.keyBy(t => t._1)

    val Dvalue =sc.textFile(hdfsFilePath2, 3)
    val tblRdd2 =Dvalue.map(line => line.split(","))

    val firstSelRdd2= tblRdd2.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DvalueRdd = firstSelRdd2.map(selData => {
      val did = selData._1.toInt
      val data_ = selData._2
      val value = selData._3.toDouble
      (did, data_, value)
    })

    val Dvalue_key=DvalueRdd.keyBy(k=>k._1)
    val join_data=Device_key.join(Dvalue_key).map(t=>(t._1,t._2._1._2,t._2._1._3,t._2._2._2,t._2._2._3))
    val filte_data=join_data.filter(t=>t._1>0&& t._1<1000 &&(t._4=="NULL"||t._4=="null"))
    //filte_data.foreach(println)
    val groupby_data=filte_data.groupBy(t=>t._2)
    val sum_data=groupby_data.map(t=>(t._1,t._2.map(t=>t._5).sum))
    val order_data=sum_data.sortBy(t=>t._1)
    order_data.foreach(println)
   // order_data.repartition(1).saveAsTextFile("hdfs:///user/hadoop/result1")
  }




  def sql2(): Unit ={
    val hdfsFilePath1 = "hdfs:///user/hadoop/input/device/"
    val hdfsFilePath2 = "hdfs:///user/hadoop/input/dvalue/"
    //val hdfsFilePath1 = "file:///home/hadoop/Desktop/test1.txt"
   // val hdfsFilePath2 = "file:///home/hadoop/Desktop/test2.txt"
    val conf = new SparkConf()
    conf.setAppName("sql_2")
    val sc = new SparkContext(conf)
    val Device=sc.textFile(hdfsFilePath1, 3)
    val tblRdd1 =Device.map(line => line.split(","))

    val firstSelRdd1= tblRdd1.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DeviceRdd = firstSelRdd1.map(selData => {
      val id = selData._1.toInt
      val type_ = selData._2
      val location = selData._3
      (id, type_, location)
    })


    val Device_key= DeviceRdd.keyBy(t => t._1)

    val Dvalue =sc.textFile(hdfsFilePath2, 3)
    val tblRdd2 =Dvalue.map(line => line.split(","))

    val firstSelRdd2= tblRdd2.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DvalueRdd = firstSelRdd2.map(selData => {
      val did = selData._1.toInt
      val data_ = selData._2
      val value = selData._3.toDouble
      (did, data_, value)
    })

    val Dvalue_key=DvalueRdd.keyBy(k=>k._1)
    val join_data=Device_key.leftOuterJoin(Dvalue_key).map(t=>{
       val a=t._1
       val b=t._2._1._2
       val c=t._2._1._3
       val temp=t._2._2.getOrElse((0,"NULL",0.0))
       val d=temp._2;
       val e=temp._3;
      (a,b,c,d,e)
    })
    val filte_data=join_data.filter(t=>t._1>0&& t._1<1000 &&t._4=="NULL"||t._4=="null")
    //filte_data.foreach(println)
    val groupby_data=filte_data.groupBy(t=>t._2)

    val avg_data=groupby_data.map(t=> {
      val data="NULL"
      val type_ = t._1
      val avg=  t._2.map(t=>t._5).toSet.sum/t._2.map(t=>t._5).toSet.size
      (data,type_,avg)
    }
    )

    val order_data= avg_data.sortBy(t=>t._2)
    order_data.collect.foreach(println)

  }




  def stream(): Unit ={
    val hdfsFilePath1 = "hdfs:///user/hadoop/input/device/"
    //val hdfsFilePath1 = "file:///home/hadoop/Downloads/data/device/"
    val conf = new SparkConf().setAppName("stream")

    val sc = new SparkContext(conf)
    val Device=sc.textFile(hdfsFilePath1, 3)
    val tblRdd1 =Device.map(line => line.split(","))

    val firstSelRdd1= tblRdd1.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DeviceRdd = firstSelRdd1.map(selData => {
      val id = selData._1.toInt
      val type_ = selData._2
      val location = selData._3
      (id, type_, location)
    })
    val Device_key= DeviceRdd.keyBy(t => t._1)


    val ssc = new StreamingContext(sc, Seconds(1))

   val Dvalue= MQTTUtils.createStream(ssc, "tcp://114.55.92.31:1883", "SparkStreamingMQTT")


   val tblRdd2 =Dvalue.map(line => line.split(","))

    val firstSelRdd2= tblRdd2.map(arrays =>
      (arrays(0), arrays(1), arrays(2)))
    val DvalueRdd = firstSelRdd2.map(selData => {
      val did = selData._1.toInt
      val data_ = selData._2
      val value = selData._3.toDouble
      (did, (data_, value))
    })

    val res=DvalueRdd.transform(rdd=>{
          rdd.join(Device_key).filter(r=>r._2._1._2>100)map(r=>(r._1,r._2._2._2,r._2._2._3))
     })

    // Print the first ten elements of each RDD generated in this DStream to the console
    res.print()
   //res.saveAsTextFiles("file:///home/hadoop/Desktop/result/test.txt")
    ssc.start()
    ssc.awaitTermination()
  }

 def main(args: Array[String]): Unit ={
   // sql1();
    sql2();
   //stream();
  }
}
