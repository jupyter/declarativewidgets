/**
  * Copyright (c) Jupyter Development Team.
  * Distributed under the terms of the Modified BSD License.
  */

package urth.widgets.query

import org.apache.spark.SharedSparkContext
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json._

class QuerySupportSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterAll with SharedSparkContext {

  class TestQuerySupport extends QuerySupport

  val support = spy(new TestQuerySupport)

  case class TestRow(val letters: String, val numbers: Int)

  object TestRow {
    def from(line: String): TestRow = {
      val f = line.split(",")
      TestRow(f(0), f(1).trim().toInt)
    }
  }

  var queryDf:DataFrame = _

  override def beforeAll() = {
    super.beforeAll()
    val sqlCtx = new SQLContext(sc)
    queryDf = sqlCtx.createDataFrame(Seq("A,1", "B,2", "C,3", "C,4", "C,5", "D,6").map(TestRow.from))
  }

  describe("Dataframe Query Support") {
    it("should apply_query_single_filter") {
      val query = """[{"type":"filter","expr":"letters = 'C'"}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.count() == 3)
    }
    it("should apply_query_multiple_filters") {
        val query = """[{"type":"filter","expr":"letters = 'C'"}, {"type":"filter","expr":"numbers = '4'"}]"""
        val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
        val newDf = support.applyQuery(queryDf, queryJson)
        assert(newDf.count() == 1)
    }
    it("should apply_query_sort_ascending_false") {
      val query = """[{"type":"sort","expr":{"by":"numbers","ascending":false}}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.select("numbers").rdd.map(r => r(0).asInstanceOf[Int]).collect()(0) == 6)
    }
    it("should apply_query_sort_ascending_true") {
      val query = """[{"type":"sort","expr":{"by":"numbers","ascending":true}}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.select("numbers").rdd.map(r => r(0).asInstanceOf[Int]).collect()(0) == 1)
    }
    it("should apply_query_sort_and_filter") {
      val query ="""[{"type":"filter","expr":"letters = 'C'"}, {"type":"sort","expr":{"by":"numbers","ascending":false}}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.select("numbers").rdd.map(r => r(0).asInstanceOf[Int]).collect()(0) == 5)
    }
    it("should apply_query_groupby") {
      val query = """[{"type":"group","expr":{"by":["letters"],"agg":[{"op":"sum","col":"numbers"},{"op":"mean","col":"numbers"}]}}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.count() == 4)
      assert(newDf.select("sum(numbers)").rdd.map(r => r(0).asInstanceOf[Long]).collect()(2) == 12)
      assert(newDf.select("mean(numbers)").rdd.map(r => r(0).asInstanceOf[Double]).collect()(2) == 4)
    }
    it("should apply_query_groupby_and_filter") {
      val query = """[{"type":"filter","expr":"letters = 'C'"}, {"type":"group","expr":{"by":["letters"],"agg":[{"op":"sum","col":"numbers"},{"op":"mean","col":"numbers"}]}}]"""
      val queryJson = Json.parse(query).asOpt[JsArray].getOrElse(new JsArray())
      val newDf = support.applyQuery(queryDf, queryJson)
      assert(newDf.count() == 1)
      assert(newDf.select("sum(numbers)").rdd.map(r => r(0).asInstanceOf[Long]).collect()(0) == 12)
      assert(newDf.select("mean(numbers)").rdd.map(r => r(0).asInstanceOf[Double]).collect()(0) == 4)
    }
  }
}