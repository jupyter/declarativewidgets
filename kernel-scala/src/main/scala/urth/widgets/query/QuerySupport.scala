package urth.widgets.query

import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions.{expr, col}
import org.apache.toree.utils.LogLike
import play.api.libs.json._

trait QuerySupport extends LogLike {

  def applyQuery( df: DataFrame, query: JsArray = new JsArray()): DataFrame = {
    implicit var newdf = df

    query.as[List[JsObject]].foreach( item =>
      item \ "type" match {
        case JsString("filter") => newdf = handleFilter((item \ "expr").as[String])
        case JsString("group") => newdf = handleGroup((item \ "expr").as[JsObject])
        case JsString("sort") => newdf = handleSort((item \ "expr").as[JsObject])
        case _ => logger.warn(s"Unsupported query expression ${item}")
      }
    )

    newdf
  }

  /**
   * Handles the expression of a filter query
   * @param expr - a string with filter expression
   * @param df
   * @return
   */
  private[query] def handleFilter(expr: String)(implicit df: DataFrame): DataFrame = {
    df.filter(expr)
  }

  /**
   * Handles the expression of a group query
   * @param expr - a JSON object with by and agg clauses
   * @param df
   * @return
   */
  private[query] def handleGroup(expr: JsObject)(implicit df: DataFrame): DataFrame = {
    val by = toColExpr(
      expr \ "by" match {
        case JsString(col) => List(col)
        case JsArray(colList:Seq[JsString]) => colList.map(_.as[String]).toList
        case _ => Nil
      }
    )

    val agg =  toArrayOfFuncExpr(
      expr \ "agg"  match {
        case anAgg:JsObject => List(anAgg)
        case JsArray(aggList:Seq[JsObject]) => aggList.map(_.as[JsObject]).toList
        case _ => List()

      }
    )

    df.groupBy(by:_*).agg(agg.head, agg.tail :_*)
  }

  /**
   * HAndle the expression of a sort query
   * @param expr
   * @param df
   * @return
   */
  private[query] def handleSort(expr: JsObject)(implicit df: DataFrame): DataFrame = {
    val by = toColExpr(
      expr \ "by" match {
        case JsString(col) => List(col)
        case JsArray(colList:Seq[JsString]) => colList.map(_.as[String]).toList
        case _ => Nil
      })

    val ascending =
      expr \ "ascending"  match {
        case JsBoolean(asc) => List(asc)
        case JsArray(ascList:Seq[JsBoolean]) => ascList.map(_.as[Boolean]).toList
        case _ => Nil
      }

    df.orderBy(
      (by zip ascending).map{
        case(c,true) => c.asc
        case(c,false) => c.desc
      } :_*
    )
  }

  private[query] def toColExpr(list:List[String]): List[Column] = {
    list.map(col(_))
  }

  private[query] def toArrayOfFuncExpr(list:List[JsObject]):List[Column] = {
    list.map {
      case agg: JsObject => "%s(%s)".format((agg \ "op").as[String],(agg \ "col").as[String])
    }.map {
      expr(_)
    }
  }

}
