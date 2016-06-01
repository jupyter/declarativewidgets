/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets.util

import org.apache.toree.utils.LogLike
import org.apache.spark.sql.DataFrame
import play.api.libs.json._
import urth.widgets.Default

/**
 * Contains methods for serializing a variable based on its type.
 */
trait SerializationSupport extends LogLike {

  /**
   * Serialize the given variable to a JSON representation based on the
   * inferred runtime type.
   *
   * @param x variable to serialize
   * @param limit Bounds the output for some serializers, e.g. limits the
   *              number of rows returned for a DataFrame.
   * @return JSON representation of `x`
   */
  def serialize(x: Any, limit: Int = Default.Limit): JsValue = {
    logger.trace(s"Serializing ${x}...")
    x match {
      case d: DataFrame => dataFrameWrites(limit).writes(d)
      case x: Float      => JsNumber(x.toDouble)
      case x: Double     => JsNumber(x)
      case x: Int        => JsNumber(x)
      case x: Boolean    => JsBoolean(x)
      case x: BigDecimal => JsNumber(x)
      case s: Seq[Any]   => JsArray((s map(serialize(_, limit))))
      case a: Array[Any] => JsArray((a map(serialize(_, limit))))
      case t: Product    => JsArray((t.productIterator.toList map(serialize(_, limit))))
      case m: Map[_, _]  => JsObject(
        m.map(p => (p._1.toString, serialize(p._2, limit))).toSeq
      )
      case j: JsValue => j
      case _ => Json.toJson(x.toString)
    }
  }

  /**
   * Serializer for a Spark DataFrame.
   * @param limit Maximum number of rows to include in the serialization.
   * @return Writes function used to convert a DataFrame to JSON.
   */
  def dataFrameWrites(limit: Int) = Writes {
    (df: DataFrame) => {
      val columns: Array[String] = df.columns

      val data: Array[Array[JsValue]] = df.toJSON.take(limit).map( jsonRow => toArray(Json.parse(jsonRow).as[JsObject], columns))

      val index: Array[String] = (0 until data.length).map(_.toString).toArray

      Json.obj(
        "columns" -> columns,
        "index"   -> index,
        "data"    -> data
      )
    }
  }

  /**
   * Transform the JsObject into an Array of JsValue ordered by the columns array.
   * @param jsonRow
   * @param columns
   * @return
   */
  def toArray( jsonRow:JsObject, columns:Array[String] ): Array[JsValue] = {
    columns.map(fieldName => jsonRow \ fieldName)
  }

}
