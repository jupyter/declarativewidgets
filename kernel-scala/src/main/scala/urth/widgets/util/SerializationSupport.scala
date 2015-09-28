/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets.util

import com.ibm.spark.utils.LogLike
import org.apache.spark.sql.DataFrame
import play.api.libs.json.{Json, JsValue, Writes}
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
      val data: Array[Array[String]] =
        df.take(limit).map(row => row.toSeq.toArray.map(_.toString))
      val index: Array[String] = (0 until data.length).map(_.toString).toArray

      Json.obj(
        "columns" -> columns,
        "index"   -> index,
        "data"    -> data
      )
    }
  }

}
