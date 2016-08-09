/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets.util

import declarativewidgets._
import org.apache.toree.interpreter.Interpreter
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

import org.apache.spark.repl.SparkIMain

import org.apache.toree.utils.LogLike

import scala.reflect.runtime.universe._

/**
 * Contains methods for function invocation and obtaining function metadata.
 */
trait FunctionSupport {

  // Maps argument names to properties, e.g. type
  type Signature = Map[String, Map[String, JsValue]]

  /**
   * Invokes a function that is in the interpreter's context with the given
   * argument map. The function can either be a method declared with def,
   * or a function val. Arguments are represented as Strings and are
   * converted to their inferred types based on the method's type signature.
   *
   * @param funcName Name of the function to invoke.
   * @param args Map[ArgumentName, ArgumentValue]
   * @return Result of invoking the given function with the given arguments
   *         converted to their inferred types or Failure if invocation fails.
   */
  def invokeFunction(funcName: String, args: Map[String, String]): Try[Any]

  /**
   * Invoke the given watch handler with the given arguments. Arguments are
   * converted from JSON values into their corresponding Scala types prior
   * to invocation.
    *
    * @param arg1Json JSON representation of the first handler argument.
   * @param arg2Json JSON representation of the second handler argument.
   * @param handler Handler function to invoke.
   * @return Some(()) if successful, None if an error occurs during invocation.
   */
  def invokeWatchHandler(
    arg1Json: JsValue, arg2Json: JsValue, handler: WatchHandler[_]): Option[Unit]

  /**
   * Returns a Signature describing the parameters of the function
   * with the given name. The Signature will contain a {"type" -> js_type} value
   * for each parameter name, where js_type denotes the JavaScript type name,
   * and a {"required" -> true|false} value denoting whether the argument
   * must be present prior to function invocation.
   *
   * Returns None when the function cannot be found in the interpreter.
    *
    * @param funcName The name of the function.
   * @return Signature or None.
   */
  def signature(funcName: String): Option[Signature]
}

/**
 * Represents the standard implementation of FunctionSupport
 * using the kernel interpreter's SparkIMain instance and Scala reflection.
 */
trait StandardFunctionSupport extends FunctionSupport with LogLike {

  lazy val iMain: SparkIMain = sparkIMain
  lazy val kernelInterpreter: Interpreter = getKernel.interpreter

  /**
   * Invokes a function that is in the interpreter's context with the given
   * argument map. The function can either be a method declared with def,
   * or a function val. Arguments are represented as Strings and are
   * converted to their inferred types based on the method's type signature.
   *
   * @param funcName Name of the function to invoke.
   * @param args Map[ArgumentName, ArgumentValue]
   * @return Result of invoking the given function with the given arguments
   *         converted to their inferred types or Failure if invocation fails.
   */
  override def invokeFunction(
    funcName: String, args: Map[String, String]
  ): Try[Any] = tryGetSymbol(funcName) flatMap { case symbol =>
      symbol.kindString match {
        case SymbolKind.Method => invokeMethod(funcName, args)
        case SymbolKind.Value  => invokeVal(funcName, args)
        case s => throw new RuntimeException(
          s"Unsupported symbol kind $s for function $funcName"
        )
      }
    }

  /**
   * Returns a Signature describing the parameters of the function
   * with the given name. The Signature will contain a {"type" -> js_type} value
   * for each parameter name, where js_type denotes the JavaScript type name,
   * and a {"required" -> true|false} value denoting whether the argument
   * must be present prior to function invocation. A {"value" -> default_value}
   * value is included when the parameter has a default value.
   *
   * Returns None when the function cannot be found in the interpreter.
    *
    * @param funcName The name of the function.
   * @return Signature or None.
   */
  override def signature(funcName: String): Option[Signature] =
    argTypes(funcName).flatMap(types => {
      argDefaults(funcName).map(defaults => {
        types.foldLeft(Map.empty: Signature) {
          case (map, (name, tpe)) =>
            map + (name -> (Map(
              Comm.KeyType -> JsString(jsType(tpe)),
              Comm.KeyRequired -> JsBoolean(!defaults.contains(name))
            ) ++ defaults.get(name).map(d =>
              (Comm.KeyDefaultValue, JsString(d.toString))
            )))
        }
      })
    })

  /**
   * Returns a list containing a (parameter_name, parameter_type) pair for
   * each parameter of the function with the given name.
   *
   * Returns None when the function cannot be found in the interpreter.
    *
    * @param funcName The name of the function.
   * @return List of (parameter_name, parameter_type) or None.
   */
  private[util] def argTypes(funcName: String): Option[List[(String, Type)]] =
    getSymbol(funcName) match {
      case Some(symbol) => {
        logger.trace(s"Found symbol $symbol")
        val spec = symbol.kindString match {
          case "method" => defArgTypes(symbol)
          case "value"  => valArgTypes(funcName)
          case _ => None
        }
        logger.trace(s"argTypes for $funcName: ${spec}")
        spec
      }
      case None =>
        logger.trace(s"No symbol found for function ${funcName}")
        None
    }

  /**
   * Returns a map of parameter_name -> parameter_default_value for
   * each default parameter of the function with the given name.
    *
    * @param funcName The name of the function.
   * @return Map of parameter_name -> parameter_default_value or None.
   */
  private[util] def argDefaults(funcName: String): Option[Map[String, Any]] =
    getSymbol(funcName) match {
      case Some(symbol) => symbol.kindString match {
        case "method" => defArgDefaults(symbol, funcName)
        case "value"  => Some(Map())
        case _ => None
      }
      case None => None
    }

  /**
   * Matches argument values with their respective types. Uses a value from
   * `defaults` if the argument does not appear in `args`.
   *
   * @param spec list of (argument_name, argument_type)
   * @param args map of (argument_name -> argument_value)
   * @param defaults map of (argument_name -> default_argument_value)
   * @return Sequence of (argument_value, argument_type) or None
   */
  private[util] def matchArgs(
    spec: List[(String, Type)],
    args: Map[String, String],
    defaults: Map[String, Any]
  ): Option[Seq[(String, Type)]] = {
    val argOpts = spec.map {
      case (name, tpe) => args.get(name) match {
        case Some(value) => Some((value, tpe))
        case None =>
          defaults.get(name) match {
            case Some(default) => Some((default.toString, tpe))
            case None =>
              logger.error(s"Argument map ${args} does not contain arg ${name}")
              None
          }
      }
    }
    if (argOpts.exists(_.isEmpty)) None else Some(argOpts.map(_.get))
  }

  /**
   * Converts the given argument values into the given argument types.
   *
   * @param args sequence of (argument_value, argument_type)
   * @return Sequence of converted_arg_value for each argument or None if
   *         any conversion fails.
   */
  private[util] def convertArgs(args: Seq[(String, Type)]): Option[Seq[Any]] = {
    val argOpts = args.map { case (value, tpe) => convertArg(value, tpe) }
    if (argOpts.exists(_.isEmpty)) None else Some(argOpts.map(_.get))
  }

  /**
   * Convert an argument represented as a string into
   * the given type.
   *
   * NOTE: def parameter types are inferred using iMain.global types, while
   *       val parameter types are inferred using scala types, hence the double
   *       check. This could potentially be simplified if def types are
   *       inferred without using iMain, or if val types were inferred using
   *       iMain instead of reflection.
   *
   * @return converted value or None if conversion fails
   **/
  private[util] def convertArg(value: String, tpe: Type): Option[Any] = {
    val gTpe = tpe.asInstanceOf[iMain.global.Type]
    val result = Try(gTpe match {
      case gTpe if gTpe =:= iMain.global.typeOf[Int]     ||
        tpe =:= typeOf[Int]     => value.toInt
      case gTpe if gTpe =:= iMain.global.typeOf[Double]  ||
        tpe =:= typeOf[Double]  => value.toDouble
      case gTpe if gTpe =:= iMain.global.typeOf[Float]   ||
        tpe =:= typeOf[Float]   => value.toFloat
      case gTpe if gTpe =:= iMain.global.typeOf[Boolean] ||
        tpe =:= typeOf[Boolean] => value.toBoolean
      case gTpe if gTpe =:= iMain.global.typeOf[Short]   ||
        tpe =:= typeOf[Short]   => value.toShort
      case gTpe if gTpe =:= iMain.global.typeOf[Byte]    ||
        tpe =:= typeOf[Byte]    => value.toByte
      case gTpe if gTpe =:= iMain.global.typeOf[Char]    ||
        tpe =:= typeOf[Char]    => value.toCharArray.head
      case gTpe if gTpe =:= iMain.global.typeOf[runtime.BoxedUnit] ||
        tpe =:= typeOf[runtime.BoxedUnit] => ()
      case gTpe if gTpe =:= iMain.global.typeOf[JsArray] ||
        tpe =:= typeOf[JsArray] => convertStringArray(value)
      case gTpe if gTpe =:= iMain.global.typeOf[JsObject] ||
        tpe =:= typeOf[JsObject] => convertStringObject(value)
      case gTpe if gTpe.toString == typeOf[String].toString => value
      case _ =>
        logger.warn(
          s"Could not detect type of ${gTpe} ${value}, returning as String"
        )
        value
    })
    result match {
      case Success(res) => Some(res)
      case Failure(t) =>
        logger.error(s"Exception when converting $value to $tpe: $t.getMessage")
        None
    }
  }

  /**
   * Convert an array String, e.g. [1, 2, 3] to a JsArray.
    *
    * @param value array String
   * @return JsArray representing the array found in the String
   */
  private[util] def convertStringArray(value: String): JsArray =
    Json.parse(value).as[JsArray]

  /**
   * Convert an object String, e.g. {"a": 1} to a JsObject.
    *
    * @param value object String
   * @return JsObject representing the object found in the String
   */
  private[util] def convertStringObject(value: String): JsObject =
    Json.parse(value).as[JsObject]

  /**
   * Converts the given value to an AnyRef when possible.
    *
    * @param value
   * @return AnyRef representation of the given value
   */
  private[util] def toAnyRef(value: Any): AnyRef = value match {
    case v: scala.Int     => new java.lang.Integer(v)
    case v: scala.Double  => new java.lang.Double(v)
    case v: scala.Float   => new java.lang.Float(v)
    case v: scala.Long    => new java.lang.Long(v)
    case v: scala.Short   => new java.lang.Short(v)
    case v: scala.Byte    => new java.lang.Byte(v)
    case v: scala.Char    => new java.lang.Character(v)
    case v: scala.Boolean => new java.lang.Boolean(v)
    case v: scala.Unit    => null
    case v: AnyRef => v
    case _ => throw new IllegalArgumentException(
      s"$value cannot be converted to an AnyRef"
    )
  }

  /**
   * Find the name of the JavaScript type corresponding to the given Scala Type.
    *
    * @param tpe Scala Type
   * @return name of JavaScript type for tpe
   */
  private[util] def jsType(tpe: Type): String =
    tpe.asInstanceOf[iMain.global.Type] match {
      case gTpe if gTpe =:= iMain.global.typeOf[Int] ||
        tpe =:= typeOf[Int]      => "Number"
      case gTpe if gTpe =:= iMain.global.typeOf[Double] ||
        tpe =:= typeOf[Double]   => "Number"
      case gTpe if gTpe =:= iMain.global.typeOf[Float] ||
        tpe =:= typeOf[Float]    => "Number"
      case gTpe if gTpe =:= iMain.global.typeOf[Short] ||
        tpe =:= typeOf[Short]    => "Number"
      case gTpe if gTpe =:= iMain.global.typeOf[JsArray] ||
        tpe =:= typeOf[JsArray]  => "Array"
      case gTpe if gTpe =:= iMain.global.typeOf[JsObject] ||
        tpe =:= typeOf[JsObject] => "Object"
      case _ => tpe.toString
    }

  /**
   * Convert the given JSON value to its corresponding Scala runtime type.
    *
    * @param x The JSON value to convert.
   * @return Value with converted Scala runtime type.
   */
  def convertJsValue(x: JsValue): Any = x match {
    case JsNumber(v)   => if (v.isValidInt) v.toInt else v.toDouble
    case JsString(v)   => v
    case JsBoolean(v)  => v
    case JsArray(v)    => (v map convertJsValue).toSeq
    case JsObject(v)   => (v map (t => (t._1, convertJsValue(t._2)))).toMap
    case JsUndefined() => None
    case JsNull        => None
    case _ => None
  }

  /**
   * Retrieve the class instance for the interpreter code request that
   * contained the declaration of the method with the given name.
    *
    * @param methodName
   * @return Request instance or None if no request is found
   */
  private[util] def requestReadClassInstance(
    methodName: String
  ): Option[AnyRef] = {
    getRequest(methodName) map { case request => {
      val readClassName = request.lineRep.readPath + "$"
      val iMainClassLoader = iMain.getClass.getDeclaredMethod("classLoader")
        .invoke(iMain).asInstanceOf[ClassLoader]
      val requestReadClassInstance = Class.forName(
        readClassName, true, iMainClassLoader
      )
      val requestReadClassConstructor =
        requestReadClassInstance.getDeclaredConstructor()
      requestReadClassConstructor.setAccessible(true)
      val requestReadInst = requestReadClassConstructor.newInstance()
      val instMethod = requestReadInst.getClass.getDeclaredMethod("INSTANCE")
      val instance = instMethod.invoke(requestReadInst)
      instance
    }}
  }

  /**
    * Invokes the class level method by getting a handle of the class from the kernel and invoking the declared method
    *
    * @param obj instance of request read class for the given method name
    * @param className name of the class to invoke
    * @param methodName name of the method to invoke
    * @param args
    * @return result of invocation
    */
  private[util] def evalClassLevelMethod(
                                       obj: AnyRef, className: String, methodName: String, args: AnyRef*
                                     ): AnyRef = {
    kernelInterpreter.read(className) match {
      case Some(klass) => {
        val theMethod = klass.getClass.getDeclaredMethods.filter(_.getName.equals(methodName)).last
        theMethod.invoke(klass, args: _*)
      }
      case None => {
        logger.trace(s"Interpreter could not find the class $className used to look for the method $methodName")
        None
      }
    }
  }

  /**
    * Invokes the method by recursively calling iw() on the Request instance
    * until the real method is reached.
    *
    * @param obj instance of request read class for the given method name
    * @param methodName name of the method to invoke
    * @param args
    * @return result of invocation
    */
  private[util] def evalDefinedMethod(
                                       obj: AnyRef, methodName: String, args: AnyRef*
                                     ): AnyRef = {
    val iwMethod = obj.getClass.getDeclaredMethods.find(
      _.getName.endsWith("$iw")
    )

    iwMethod match {
      case Some(method) =>
        evalMethod(method.invoke(obj), methodName, args: _*)
      case None =>
        val method = obj.getClass.getDeclaredMethods.find(
          _.getName.endsWith(methodName)
        )
        method.get.invoke(obj, args: _*)
    }
  }

  /**
    * Invokes the method by recursively calling iw() on the Request instance
    * until the real method is reached.
    *
    * @param obj instance of request read class for the given method name
    * @param methodName name of the method to invoke
    * @param args
    * @return result of invocation
    */
  private[util] def evalMethod(
                                obj: AnyRef, methodName: String, args: AnyRef*
                              ): AnyRef = {
    val methodNameParts = methodName.split('.')
    if (methodNameParts.length > 1) {
      evalClassLevelMethod(obj, methodNameParts(0), methodNameParts(1), args: _*)
    }
    else {
      evalDefinedMethod(obj, methodNameParts(0), args: _*)
    }
  }

  /**
   * Invoke the method with the given name using the given arguments.
    *
    * @param methodName
   * @param args
   * @return result of method invocation or None if the method was not found.
   */
  private[util] def _invokeMethod(
    methodName: String, args: Any*
  ): Option[AnyRef] = {
    requestReadClassInstance(methodName) map { case inst => {
      val refArgs = args map toAnyRef
      evalMethod(inst, methodName, refArgs: _*)
    }}
  }

  /**
   * Invokes a method that is in the interpreter's context with the given
   * argument map.
   *
   * @param methodName name of the method to invoke
   * @param args mapping of argument name to argument value as a string
   * @return Result of method invocation, or Failure if invocation fails
   */
  private[util] def invokeMethod(
    methodName: String, args: Map[String, String]
  ): Try[Any] = {
    logger.trace(s"invokeMethod with function $methodName and args $args")
    tryOptionFunction(() => for {
      spec     <- argTypes(methodName)
      symb     <- getSymbol(methodName)
      defaults <- defArgDefaults(symb, methodName)
      argSeq   <- matchArgs(spec, args, defaults)
      convArgs <- convertArgs(argSeq)
      result   <- _invokeMethod(methodName, convArgs: _*)
    } yield result, methodName)
  }

  /**
   * Invokes a function val that is in the interpreter's context with the given
   * argument map.
   *
   * @param funcName name of the variable containing the function to invoke
   * @param args mapping of argument name to argument value as a string
   * @return Result of function invocation, or Failure if invocation fails
   */
  private[util] def invokeVal(
  funcName: String, args: Map[String, String]
  ): Try[Any] = {
    logger.trace(s"invokeVal with function $funcName and args $args")
    tryOptionFunction(() => for {
        func      <- kernelInterpreter.read(funcName)
        (im, sym) <- applySymbol(func)
        spec      <- argTypes(funcName)
        argSeq    <- matchArgs(spec, args, Map())
        convArgs  <- convertArgs(argSeq)
      } yield im.reflectMethod(sym)(convArgs: _*), funcName)
  }

  /**
   * Execute the given function that returns an Option type, catching execution
   * errors.
    *
    * @param f The function to execute.
   * @param funcName The name of the function to execute.
   * @tparam T Type of underlying data that the function `f` returns.
   * @return Success(function result data) or Failure if execution fails.
   */
  private[util] def tryOptionFunction[T](f: () => Option[T], funcName: String): Try[T] =
    Try(f() getOrElse {
      throw new RuntimeException(s"Error invoking function $funcName")}
    )

  /**
   * Invoke the given watch handler with the given arguments. Arguments are
   * converted from JSON values into their corresponding Scala types prior
   * to invocation.
    *
    * @param arg1Json JSON representation of the first handler argument.
   * @param arg2Json JSON representation of the second handler argument.
   * @param handler Handler function to invoke.
   * @return Some(()) if successful, None if an error occurs during invocation.
   */
  override def invokeWatchHandler(
    arg1Json: JsValue, arg2Json: JsValue, handler: WatchHandler[_]
  ): Option[Unit] = {
    val arg1 = wrapValueInOption(convertJsValue(arg1Json))
    val arg2 = convertJsValue(arg2Json)
    logger.trace(s"Invoking handler with arguments $arg1, $arg2")
    applySymbol(handler) flatMap {
      case (im, sym) => Try(im.reflectMethod(sym)(arg1, arg2)) match {
        case Success(v) => Some(())
        case Failure(t) =>
          logger.error(s"Error invoking watch handler: ${t.getMessage}")
          None
      }
    }
  }

  private[util] def wrapValueInOption(x: Any): Option[Any] = x match {
    case None => None
    case _ => Some(x)
  }

  /**
    * Retrieves the class name (String) of the defined names value definition
    * e.g. t = new Test() -> Test, given t as the name
    *
    * @param name defined name to get the class value definition name from
    * @return Some(class value definition name) or None if request was not found
    */
  private[util] def classNameOfRequest(name: String) = {
    getRequest(name) match {
      case Some(req) => {
        val valDef = req.trees.head.asInstanceOf[reflect.runtime.universe.Tree] collect{case c: ValDef => c.rhs}
        val regex = "new ".r
        valDef.lastOption.map(x => {
          val className = regex.replaceFirstIn(x.toString(), "")
          className.substring(0, className.indexOf("("))
        })
      }
      case None => None
    }
  }

  /**
    * Retrieves the Method Symbol if found under the members of the given class name
    *
    * @param className class name to look for the method symbol under
    * @param methodName retrieve the method symbol under this name given the class
    * @return Some(method symbol) or None if the method symbol was not found
    */
  private[util] def getMethodSymbol(className: String, methodName: String): Option[iMain.global.Symbol] = {
    iMain.classSymbols.filter(klass => {
      val klassName = klass.nameString
      klassName.substring(klassName.lastIndexOf('$')+1).equals(className)
    }).lastOption match {
      case Some(classSym) => {
        classSym.toType.members.filter(_.nameString.equals(methodName)).lastOption
      }
      case None => {
        logger.trace(s"No symbol found for $className.$methodName")
        None
      }
    }
  }

  /**
    * Retrieves the Symbol representing the item (e.g. variable, method) with
    * the given name. If multiple symbols exist for the given name, chooses
    * the most recently declared symbol.
    *
    * @param symbolName retrieve the Symbol corresponding to this name
    * @return Some(symbol for name) or None if no symbol was found
    */
  private[util] def getDefinedSymbol(symbolName: String): Option[iMain.global.Symbol] = {
    val symbolList = iMain.definedSymbolList
    logger.trace(s"Getting symbol for $symbolName. Symbols: ${symbolList}")
    symbolList.filter(_.nameString == symbolName).toList match {
      case Nil =>
        logger.trace(s"No symbol found for $symbolName")
        None
      case lst =>
        val sym = lst.last
        logger.trace(s"Symbol $sym found for $symbolName")
        Some(sym)
    }
  }

  /**
    * Retrieves the symbol representing the item (e.g. variable, method, method within object)
    * with the given name.  If multiple symbols exist for the given name, chooses the most
    * recently declared symbol.  This method will delegate if the symbol is in the top level
    * user defined symbol list or if it is nested under a class symbol.
    *
    * @param name retrieve teh Symbol corresponding to this name
    * @return Some (symbol for name) or None if no symbol was found
    */
  private[util] def getSymbol(name: String): Option[iMain.global.Symbol] = {
    val symbolParts = name.split('.')
    if (symbolParts.length > 1) {
      val className = classNameOfRequest(symbolParts(0))
      className match {
        case Some(cName) => {
          getMethodSymbol(cName, symbolParts(1))
        }
        case None => {
          logger.trace(s"No symbol found for $name")
          None
        }
      }
    }
    else {
      getDefinedSymbol(symbolParts(0))
    }
  }

  private[util] def tryGetSymbol(name: String): Try[iMain.global.Symbol] =
    getSymbol(name) match {
      case Some(sym) => Success(sym)
      case None => throw new RuntimeException(s"Symbol $name not found!")
    }

  /**
    * Get the tree corresponding to the defined request
    *
    * @param definedName name of method or val from the defined names
    * @return The AST of the request
    */
  private[util] def getTreeFromDefinedRequest(definedName: String): Option[reflect.runtime.universe.Tree] = {
    getRequest(definedName).map(_.asInstanceOf[org.apache.spark.repl.SparkIMain#Request])
      .map(_.trees.head.asInstanceOf[reflect.runtime.universe.Tree])
  }

  /**
    * Get the tree corresponding to the method request
    *
    * @param className name of the class for which the method resides in
    * @param methodName name of the method
    * @return The AST of the request
    */
  private[util] def getTreeFromMethodRequest(className: String, methodName: String): Option[reflect.runtime.universe.Tree] = {
    val requests = iMain.getClass.getDeclaredMethods.filter(_.getName.contains("prevRequestList")).head.
      invoke(iMain).asInstanceOf[List[_]].map(_.asInstanceOf[org.apache.spark.repl.SparkIMain#Request])
    val requestTrees = requests.map(_.trees.head.asInstanceOf[reflect.runtime.universe.Tree])
    val classRequestTree = requestTrees.filter(i => {
      val classDefs = i collect {case c: ClassDef => c}
      classDefs.length > 0 && classDefs.last.name.toString.equals(className)
    })
    val methodsTree = classRequestTree.map(i => {
      i collect { case v: DefDef => v }
    }).head
    methodsTree.filter(_.name.toString.equals(methodName)).lastOption
  }

  /**
    * Get the tree corresponding to the request
    *
    * @param name name of method, val, or nested method
    * @return Tree for the given defined name
    */
  private[util] def getTreeFromRequest(name: String): Option[reflect.runtime.universe.Tree] = {
    val nameParts = name.split('.')
    if (nameParts.length > 1) {
      val className = classNameOfRequest(nameParts(0))
      className match {
        case Some(cName) => {
          getTreeFromMethodRequest(cName, nameParts(1))
        }
        case None => {
          logger.trace(s"No symbol found for $name")
          None
        }
      }
    } else {
      getTreeFromDefinedRequest(nameParts(0))
    }
  }

  /** Get the interpreter Request object for the line of code that
    * contained the given name.
    *
    * @param name name of method or val
    * @return Request for the given name or None if it cannot be found
    */
  private def getRequest(name: String) = {
    val requestName = name.split('.').head
    iMain.requestForName(iMain.global.newTermName(requestName)) match {
      case Some(req) => Some(req)
      case None =>
        logger.error(s"Interpreter request for $requestName not found!")
        None
    }
  }

  /**
    * Finds the Symbol for the apply() method of the given function value.
    * Also gives the instance mirror for the function for use in further
    * reflection
    *
    * Note: uses Scala 2.10 reflection
    *
    * @param func value representing the function
    * @return Instance Mirror for the function and Symbol
    */
  private def applySymbol(func: AnyRef): Option[(InstanceMirror, MethodSymbol)] = {
    val im = runtimeMirror(getClass.getClassLoader).reflect(func)
    Try(im.symbol.typeSignature.member(newTermName("apply"))
      .asTerm.alternatives.head.asMethod) match {
      case Success(sym) => Some(im, sym)
      case Failure(t) =>
        logger.error(s"Couldn't find apply() for $func. Is $func a FunctionXX?")
        None
    }
  }

  /**
   * Returns the argument names and types for the method represented by
   * the given Symbol in the interpreter.
    *
    * @param s symbol for the method
   * @return list of (argument_name, argument_type) or None
   */
  private[util] def defArgTypes(s: iMain.global.Symbol): Option[List[(String, Type)]] =
    Some(s.typeSignature.params.map(p =>
      (p.decodedName, p.tpe.asInstanceOf[reflect.runtime.universe.Type])
    ))

  /**
   * Returns a mapping of argument name to argument default value for
   * default arguments in the method represented by the given Symbol.
    *
    * @param s symbol for the method
   * @param name name of the method
   * @return map of argument_name -> argument_default_value or None
   */
  private[util] def defArgDefaults(
    s: iMain.global.Symbol, name: String
  ): Option[Map[String, Any]] = {
    getTreeFromRequest(name) match {
      case Some(tree) =>
        val valDefs = tree collect { case v: ValDef => v }
        val defaults = s.typeSignature.params.zipWithIndex.
          filter(_._1.isParamWithDefault).map { case (p, i) => {
           (p.decodedName, extractValueFromValDef(valDefs(i)))
        }}
        if (defaults.exists(_._2.isEmpty)) None
        else Some(defaults.map(d => (d._1, d._2.get)).toMap)
      case None => None
    }
  }

  /**
   * Retrieves the right-hand-side of the given value definition.
    *
    * @param v object representing the value definition.
   * @return Value assigned in the value definition, or None.
   */
  private[util] def extractValueFromValDef(v: ValDef): Option[Any] = {
    v.collect { case t => t }.last match {
      case l: Literal =>
        logger.trace(s"Detected literal ${l.toString} as the default value")
        Some(stripOuterQuotes(l.toString))

      case i: Ident =>
        logger.trace(s"Detected ident ${i.toString} as the default value")
        kernelInterpreter.read(i.toString)

      case t =>
        logger.trace(s"Tree type ${t.getClass.getSimpleName} not supported")
        None
    }
  }

  private[util] def stripOuterQuotes(s: String): String =
    s.replaceFirst("\"", "").reverse.replaceFirst("\"", "").reverse

  /**
   * Finds the argument names and types for a function val with the given name.
    *
    * @param funcName name of the variable containing the function
   * @return list of (argument_name, argument_type) or None
   */
  private[util] def valArgTypes(funcName: String): Option[List[(String, Type)]] = for {
      types <- _valArgTypes(funcName)
      names <- valArgNames(funcName, types.size)
    } yield names.zip(types)

  /**
   * Finds the argument names for a function val with the given name.
   *
   * @param funcName name of the variable containing the function.
   * @param numArgs number of arguments that funcName has
   * @return list of argument names, or None if the names cannot be retrieved
   */
  private[util] def valArgNames(funcName: String, numArgs: Int): Option[List[String]] = {
    getTreeFromRequest(funcName) map { case tree => {
      val funcTree = (tree collect { case x: Function  => x} ).head
      val argVals  = (funcTree collect { case v: ValDef => v } ).take(numArgs)
      val argNames = argVals.map(v => v.name.decoded)
      logger.trace(s"$funcName argNames: $argNames")
      argNames
    }}
  }

  /**
   * Finds the argument types for a function val with the given name.
   *
   * Note: uses Scala 2.10 reflection
   *
   * @param funcName name of the variable containing the function.
   * @return list of argument Types, or None if the types cannot be retrieved
   */
  private[util] def _valArgTypes(funcName: String): Option[List[Type]] = {
    kernelInterpreter.read(funcName) match {
      case Some(func) =>
        val im = runtimeMirror(getClass.getClassLoader).reflect(func)
        val applySymbol = im.symbol.typeSignature.member(
          newTermName("apply")).asTerm.alternatives.head.asMethod
        val types = applySymbol.paramss.flatMap(_.map(_.typeSignature))
        logger.trace(s"Argument types for $funcName: $types")
        Some(types)
      case None =>
        logger.error(s"Function $funcName not found in kernel!")
        None
    }
  }


}
