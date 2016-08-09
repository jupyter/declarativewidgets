/**
  * Copyright (c) Jupyter Development Team.
  * Distributed under the terms of the Modified BSD License.
  */

package declarativewidgets.util

import declarativewidgets.getKernel
import declarativewidgets.sparkIMain

import org.apache.spark.sql.DataFrame
import org.apache.spark.repl.SparkIMain
import scala.reflect.runtime.universe.ValDef

/**
  * Object that contains Widget Visualization one line displays
  */

object Explore {
  /**
    * Gets the currently executing request in SparkIMain used to inspect the explore request
    *
    * @return The currently executing Request
    */
  def getExecutingRequest(): Option[org.apache.spark.repl.SparkIMain#Request]= {
    val iMain: SparkIMain = sparkIMain
    val requests = iMain.getClass.getDeclaredMethods.filter(_.getName.contains("prevRequestList")).head.
      invoke(iMain).asInstanceOf[List[_]].map(_.asInstanceOf[org.apache.spark.repl.SparkIMain#Request])
    iMain.requestForReqId(requests.last.reqId+1)
  }

  /**
    * Gets the DataFrame variable String representation passed into the currently running execute request
    *
    * @return The DataFrame variable as a String
    */
  def getDfNameFromLastExploreRequest(): String = {
    val executingRequest = getExecutingRequest()
    val requestTrees = executingRequest.map(_.trees.head.asInstanceOf[reflect.runtime.universe.Tree])
    val valDefTrees = requestTrees collect {case c: ValDef => c}
    val exploreTrees = valDefTrees.filter({ eTree =>
      val children = eTree.rhs.children
      children.length > 0 && children.head.toString.equals("explore")
    })
    exploreTrees.last.rhs.children(1).toString
  }

  /**
    * Display/renderer for the kernel
    *
    * @param df DataFrame reference as a String
    * @param channel channel reference as a String
    * @param properties properties as a String
    * @param bindings bindings as a String
    */
  def display(df: String, channel: String, properties: String, bindings: String) = {
    val explorerImport =
      """
        <link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
          is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
      """
    getKernel.display.html(s"$explorerImport " +
                            s"<template is='urth-core-bind' channel='$channel'>" +
                              s"<urth-viz-explorer ref='$df' $properties $bindings></urth-viz-explorer>" +
                            "</template>")
  }

  def stringifyProperties(properties: Map[String, Any]) = {
    properties.map {
      case (k, v) => {
        v match {
          case isProp: Boolean => if(isProp) k else ""
          case _ => k + "=" + v
        }
      }
    }.mkString(" ")
  }

  def stringifyBindings(bindings: Map[String, String]) = {
    bindings.map {
      case (k, v) => {
        s"$k={{$v}}"
      }
    }.mkString(" ")
  }

  /**
    * Renders the urth-viz-explorer widget to the user output
    *
    * @param df The (Spark DataFrame or String of the variable)
    * @param channel The channel to bind to defaulted to default
    * @param properties Map of properties e.g. {'selection-as-object': False, 'foo': 5}
    * @param bindings Map of bindings e.g. {'selection': 'sel'}
    */
  def explore(df: Any, channel: String, properties: Map[String, Any], bindings: Map[String, String]) = {
    df match {
      case d : DataFrame => {
        val dfString = getDfNameFromLastExploreRequest()
        display(dfString, channel, stringifyProperties(properties), stringifyBindings(bindings))
      }
      case s: String => {
        display(s, channel, stringifyProperties(properties), stringifyBindings(bindings))
      }
    }
  }
}
