/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets.util

import java.io.{OutputStream, ByteArrayOutputStream}

import com.ibm.spark.global.StreamState
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.interpreter.scala._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json._
import scala.reflect.runtime.universe._
import scala.runtime.BoxedUnit
import scala.util.Success

class FunctionSupportSpec extends FunSpec with Matchers with MockitoSugar {

  class TestFunctionSupport extends StandardFunctionSupport {
    override lazy val iMain = {
      val sparkIMainMethod = interpreter.getClass.getMethod("sparkIMain")
      sparkIMainMethod.invoke(interpreter).asInstanceOf[org.apache.spark.repl.SparkIMain]
    }
  }

  private var interpreter: Interpreter = _
  private val outputResult = new ByteArrayOutputStream()

  interpreter = new ScalaInterpreter(Nil, mock[OutputStream])
    with StandardSparkIMainProducer
    with StandardTaskManagerProducer
    with StandardSettingsProducer
  interpreter.start()

  StreamState.setStreams(outputStream = outputResult)

  // define test method
  interpreter.interpret("def f(a: Int): Int = a")
  val TestMethodName = "f"

  // define test val function
  interpreter.interpret("val fv = (a: Int) => a")
  val TestValName = "fv"

  // default literal value
  interpreter.interpret(s"""def fd(a: Int, b: Double=3.0, c: String="foo"): Int = a""")
  val TestDefaultMethodName = "fd"

  // default variable value
  interpreter.interpret(s"""val d: Double = 3.0""")
  interpreter.interpret(s"""def fdv(a: Int, b: Double=d): Int = a""")
  val TestDefaultVariableMethodName = "fdv"

  // method that causes an exception
  interpreter.interpret("def fme(a: Int): Int = throw new Exception")
  val TestMethodWithException = "fme"

  // val that causes an exception
  interpreter.interpret("val fve = (a: Int) => 1/0")
  val TestValWithException = "fve"

  describe("FunctionSupport") {
    val support = spy(new TestFunctionSupport)
    doReturn(interpreter).when(support).kernelInterpreter

    describe("invokeFunction") {
      it("should invoke a method defined with def") {
        support.invokeFunction(TestMethodName, Map("a" -> "1")) should be (Success(1))
      }

      it("should invoke a method defined with def using default arguments") {
        support.invokeFunction(TestDefaultMethodName, Map("a" -> "1")) should be (Success(1))
      }

      it("should invoke a function defined with val") {
        support.invokeFunction(TestValName, Map("a" -> "1")) should be (Success(1))
      }
      
      it("should return Failure if an error occurs") {
        // empty args
        support.invokeFunction(TestValName, Map()).isFailure should be (true)
      }

      it("should return Failure if an exception occurs during method invocation") {
        support.invokeFunction(TestMethodWithException, Map("a" -> "1")).isFailure should be (true)
      }

      it("should return Failure if an exception occurs during val invocation") {
        support.invokeFunction(TestValWithException, Map("a" -> "1")).isFailure should be (true)
      }
    }

    describe("argTypes") {
      it("should return argument names and types for a def method") {
        // TODO defArgTypes requires equality checks with iMain.global Types
        val expected = Some(List(("a", support.iMain.global.typeOf[Int])))
        support.argTypes(TestMethodName) should be (expected)
      }

      it("should return argument names and type for a val function") {
        val expected = Some(List(("a", typeOf[Int])))
        support.argTypes(TestValName) should be (expected)
      }

      it("should return None if an error occurs") {
        // the function does not exist
        support.argTypes("DNE") should be (None)
      }
    }

    describe("argDefaults") {
      it("should go find the defaults for a def"){
        val supportSpy = spy(new TestFunctionSupport)
        supportSpy.argDefaults(TestDefaultMethodName)

        verify(supportSpy).defArgDefaults(any(), anyString())
      }

      it("should return an empty map for a val because vals cant have defaults"){
        support.argDefaults(TestValName) should be (Some(Map()))
      }
    }

    describe("signature") {

      val msg1 = Some(Map("a" -> Map(
        "type" -> JsString("Number"),
        "required" -> JsBoolean(true)
      )))

      val msgDefault = Some(Map(
        "a" -> Map(
        "type" -> JsString("Number"),
        "required" -> JsBoolean(true)
      ),
        "b" -> Map(
          "type" -> JsString("Number"),
          "required" -> JsBoolean(false),
          "value" -> JsString("3.0")
        ),
        "c" -> Map(
          "type" -> JsString("String"),
          "required" -> JsBoolean(false),
          "value" -> JsString("foo")
        )
      ))

      it("should return argument names and types for a def method") {
        val expected = msg1.get.get("a").get("type")
        support.signature(TestMethodName).get.get("a").get("type") should
          be (expected)
      }

      it("should return argument names and type for a val function") {
        val expected = msg1.get.get("a").get("type")
        support.signature(TestValName).get.get("a").get("type") should
          be (expected)
      }

      it("should return required flags for a def method") {
        val expected = msg1.get.get("a").get("required")
        support.signature(TestMethodName).get.get("a").get("required") should
          be (expected)
      }

      it("should return required flags for a val function") {
        val expected = msg1.get.get("a").get("required")
        support.signature(TestValName).get.get("a").get("required") should
          be (expected)
      }

      it("should return required false for a default parameter") {
        val expected = msgDefault.get.get("b").get("required")
        support.signature(TestDefaultMethodName).get.get("b").get("required") should
          be (expected)
      }

      it("should return the default value for a default parameter") {
        val expected = msgDefault.get.get("b").get("value")
        support.signature(TestDefaultMethodName).get.get("b").get("value") should
          be (expected)

        val expected2 = msgDefault.get.get("c").get("value")
        support.signature(TestDefaultMethodName).get.get("c").get("value") should
          be (expected2)
      }

      it("should return the default value for a default parameter whose value is a variable") {
        val expected = msgDefault.get.get("b").get("value")
        support.signature(TestDefaultVariableMethodName).get.get("b").get("value") should
          be (expected)
      }

      it("should not return a value for a non-default parameter") {
        support.signature(TestDefaultMethodName).get.get("a").get.keys should
          not contain("value")
      }

      it("should return None if an error occurs") {
        // the function does not exist
        support.argTypes("DNE") should be (None)
      }

      it("should support multiple types") {
        interpreter.interpret("def jsArgSpecF(a: Int, b: Float, c: Boolean, d: play.api.libs.json.JsArray, e: java.lang.Object) = a")
        val expected = Some(Map(
          "a" -> Map("type" -> JsString("Number"), "required" -> JsBoolean(true)),
          "b" -> Map("type" -> JsString("Number"), "required" -> JsBoolean(true)),
          "c" -> Map("type" -> JsString("Boolean"), "required" -> JsBoolean(true)),
          "d" -> Map("type" -> JsString("Array"), "required" -> JsBoolean(true)),
          "e" -> Map("type" -> JsString("Object"), "required" -> JsBoolean(true))
        ))
        support.signature("jsArgSpecF") should be (expected)
      }
    }
    
  }

  describe("StandardFunctionSupport"){

    val support = spy(new TestFunctionSupport)

    describe("#convertArg") {
      it("should convert an Int string to an Int"){
        support.convertArg("1", typeOf[Int]) should be(Some(1))
      }
      it("should convert an Boolean string to a Boolean"){
        support.convertArg("true", typeOf[Boolean]) should be(Some(true))
      }
      it("should convert an Unit string to an Int"){
        support.convertArg("()", typeOf[BoxedUnit]) should be(Some(()))
      }
      it("should convert an array string to a JSArray"){
        support.convertArg("[1,2,3]", typeOf[JsArray]).get.
          isInstanceOf[JsArray] should be (true)
      }
      it("should convert an object string to a JSObject"){
        support.convertArg(s"""{"a": 2}""", typeOf[JsObject]).get.
          isInstanceOf[JsObject] should be (true)
      }

      it("should convert iMain.global.Type types"){
        val gtpe = support.iMain.global.typeOf[Int].asInstanceOf[Type]
        support.convertArg("1", gtpe) should be(Some(1))
      }

    }

    describe("#convertStringArray") {
      it("should convert an Array string to an Array"){
        support.convertStringArray("[1, 2, 3]") should be(
          Json.arr(1, 2, 3)
        )
      }
    }

    describe("#convertStringObject") {
      it("should convert an Object string to a Map[String, String]"){

        val obj = s"""{ "a": 2, "b": "3", "c": "d" }"""

        support.convertStringObject(obj) should be(
          Json.obj("a" -> 2, "b" -> "3", "c" -> "d")
        )
      }
    }

    describe("#toAnyRef") {
      it("should convert Int to AnyRef version") {
        support.toAnyRef(2).isInstanceOf[AnyRef] should be(true)
      }
      it("should do nothing with an AnyRef argument") {
        support.toAnyRef("") should be("")
      }
    }

    describe("#matchArgs") {
      it("should match argument values with argument types") {
        val spec = List(("a", typeOf[String]), ("b", typeOf[Int]))
        val args = Map("a" -> "foo", "b" -> "3")
        val expected = Some(Seq(("foo", typeOf[String]), ("3", typeOf[Int])))
        support.matchArgs(spec, args, Map()) should be(expected)
      }

      it("should return none if an argument is missing and not in the default map") {
        val spec = List(("a", typeOf[String]), ("b", typeOf[Int]))
        val args = Map("a" -> "foo")
        val expected = None
        support.matchArgs(spec, args, Map()) should be(expected)
      }

      it("should use a value from the default map when the argument is missing") {
        val spec = List(("a", typeOf[String]), ("b", typeOf[Int]))
        val args = Map("a" -> "foo")
        val defaults = Map("b" -> 3)
        val expected = Some(Seq(("foo", typeOf[String]), ("3", typeOf[Int])))
        support.matchArgs(spec, args, defaults) should be(expected)
      }
    }

    describe("#convertArgs") {
      it("should convert arguments to proper types") {

        val support2 = spy(new TestFunctionSupport)

        doReturn(Some("a")).when(support2).convertArg("a", typeOf[String])
        doReturn(Some(1)).when(support2).convertArg("1", typeOf[Int])

        val args = Seq(("a", typeOf[String]), ("1", typeOf[Int]))

        val expected = Some(Seq("a", 1))
        support2.convertArgs(args) should be (expected)
      }

      it("should return None if conversion fails for at least one argument") {
        val support2 = spy(new TestFunctionSupport)

        doReturn(None).when(support2).convertArg(any(), any())

        support2.convertArgs(Seq(("a", typeOf[String]))) should be(None)
      }
    }

    describe("#invokeMethod") {

      it("integration: should return the result of method invocation") {
        val supportSpy = spy(new TestFunctionSupport)

        supportSpy.invokeMethod(TestMethodName, Map("a" -> "1")) should be(Success(1))
      }

      it("integration: should work with a default value") {
        val supportSpy = spy(new TestFunctionSupport)

        supportSpy.invokeMethod(TestDefaultMethodName, Map("a" -> "1")) should be (Success(1))
      }

      it("should return Failure if any intermediate stage fails") {
        val supportSpy = spy(new TestFunctionSupport)

        doReturn(Some(Seq(("a", typeOf[String])))).when(supportSpy).argTypes("foo")
        doReturn(Some(3)).when(supportSpy)._invokeMethod(anyString(), any())

        doReturn(None).when(supportSpy).convertArgs(any())

        val args = Map("a" -> "asdf")
        supportSpy.invokeMethod("foo", args).isFailure should be (true)
      }

      it("should return Failure if the method invocation fails") {
        support.invokeMethod(TestMethodWithException, Map("a" -> "1")).isFailure should be (true)
      }
    }

    describe("#_invokeMethod") {
      it("should return None when a request cannot be found for the name") {
        val supportSpy = spy(new TestFunctionSupport)

        doReturn(None).when(supportSpy).requestReadClassInstance(anyString())

        supportSpy._invokeMethod("foo") should be (None)
      }

      it("should call evalMethod with AnyRef args") {
        val supportSpy = spy(new TestFunctionSupport)

        doReturn(Some("inst")).when(supportSpy).requestReadClassInstance(anyString())
        doReturn(None).when(supportSpy).evalMethod(any(), anyString(), any())

        val args = Seq("3", 1)
        val cargs = args map supportSpy.toAnyRef

        supportSpy._invokeMethod("foo", args: _*)
        verify(supportSpy).evalMethod("inst", "foo", cargs: _*)
      }

      it("should return the result of method invocation") {
        val supportSpy = spy(new TestFunctionSupport)
        val args = Seq(1)

        supportSpy._invokeMethod(TestMethodName, args: _*) should be(Some(1))
      }
    }

    describe("#invokeVal") {
      val supportSpy = spy(new TestFunctionSupport)
      doReturn(interpreter).when(supportSpy).kernelInterpreter

      it("should return the result of invoking a val function") {

        val args = Map("a" -> "1")
        supportSpy.invokeVal(TestValName, args) should be(Success(1))
      }

      it("should return Failure if an intermediate stage fails") {
        val badArgs = Map("a" -> "asdf")
        supportSpy.invokeVal(TestValName, badArgs).isFailure should be(true)
      }

      it("should return Failure if an exception occurs during val invocation") {
        support.invokeVal(TestValWithException, Map("a" -> "1")).isFailure should be (true)
      }
    }

    describe("#invokeWatchHandler") {
      val support = new TestFunctionSupport
      it ("should execute the given handler with the given arguments converted"){

        val arg1 = JsNumber(1)
        val arg2 = JsNumber(2)
        var executed = false
        val handler = (old: Option[Int], noo: Int) => executed = true

        support.invokeWatchHandler(arg1, arg2, handler) should be (Some(()))
        executed should be(true)
      }

      it ("should return None when execution fails"){
        val arg1 = JsNumber(1)
        val arg2 = JsNumber(2)
        val handler = (old: Option[Int], noo: Int) => { 1 / 0; ()}

        support.invokeWatchHandler(arg1, arg2, handler) should be (None)
      }

      it ("should return None when argument types don't match"){
        val arg1 = JsNumber(1)
        val arg2 = JsNumber(2)
        var executed = false
        val handler = (old: Option[String], noo: String) => executed = true

        support.invokeWatchHandler(arg1, arg2, handler) should be (None)
        executed should be(false)
      }
    }

    describe("#convertJsValue") {
      it("should convert an integer JsNumber to Scala Integer"){
        val x = JsNumber(3)
        val converted = support.convertJsValue(x)
        converted should be (3)
        converted.isInstanceOf[Int] should be (true)
      }
      it("should convert a decimal JsNumber to Scala Double"){
        val x = JsNumber(3.1)
        val converted = support.convertJsValue(x)
        converted should be (3.1)
        converted.isInstanceOf[Double] should be (true)
      }
      it("should convert a JsString to Scala String"){
        val x = JsString("foo")
        val converted = support.convertJsValue(x)
        converted should be ("foo")
        converted.isInstanceOf[String] should be (true)
      }
      it("should convert a JsBoolean to Scala Boolean"){
        val x = JsBoolean(true)
        val converted = support.convertJsValue(x)
        converted should equal (true)
        converted.isInstanceOf[Boolean] should be (true)
      }
      it("should convert a JsArray to Scala Seq"){
        val x = JsArray(Seq(JsNumber(1), JsNumber(2)))
        val converted = support.convertJsValue(x)
        val expected = Seq(1, 2)
        converted should be(expected)
        converted.isInstanceOf[Seq[_]] should be (true)
      }
      it("should convert a JsObject to Scala Map"){
        val x = JsObject(Seq(("a", JsNumber(1)), ("b", JsString("c"))))
        val converted = support.convertJsValue(x)
        val expected = Map("a" -> 1, "b" -> "c")
        converted should be(expected)
        converted.isInstanceOf[Map[_, _]] should be (true)
      }
      it("should convert JsUndefined to None"){
        val x = JsUndefined("err")
        val converted = support.convertJsValue(x)
        converted should be (None)
      }
      it("should convert JsNull to None"){
        val x = JsNull
        val converted = support.convertJsValue(x)
        converted should be (None)
      }
    }

    describe("#getSymbol") {
      val supportSpy = spy(new TestFunctionSupport)

      it("should return a symbol for a method that exists") {
        supportSpy.getSymbol(TestMethodName).get.simpleName.toString should
          be (TestMethodName)
      }

      it("should return the most recent symbol") {
        interpreter.interpret("def bean(a: Int) = a")
        interpreter.interpret("def bean(a: Int, b: Float) = a")
        val symb = supportSpy.getSymbol("bean").get
        // verify that it's the version with the additional Float parameter
        symb.typeSignature.params.length should be(2)
      }
    }

    describe("#defArgTypes") {
      it("should return names and types of a def") {
        val supportSpy = spy(new TestFunctionSupport)
        val symb = supportSpy.getSymbol(TestMethodName).get
        val lst = List(("a", supportSpy.iMain.global.typeOf[Int]))
        val expected = Some(lst)

        // TODO defArgTypes requires equality checks with iMain.global Types
        supportSpy.defArgTypes(symb).map(_.map(t =>
          (t._1, t._2.asInstanceOf[supportSpy.iMain.global.Type]))) should be (expected)
      }
    }

    describe("#defArgDefaults") {
      it("should return names and values of default parameters") {
        val supportSpy = spy(new TestFunctionSupport)
        val symb = supportSpy.getSymbol(TestDefaultMethodName).get
        val expected = Some(Map("b" -> "3.0", "c" -> "foo"))

        supportSpy.defArgDefaults(symb, TestDefaultMethodName) should be (expected)
      }

      it("should work for a default parameter whose value is a variable") {
        val supportSpy = spy(new TestFunctionSupport)
        doReturn(interpreter).when(supportSpy).kernelInterpreter
        val symb = supportSpy.getSymbol(TestDefaultVariableMethodName).get
        val expected = Some(Map("b" -> 3.0))

        supportSpy.defArgDefaults(symb, TestDefaultVariableMethodName) should be (expected)
      }

      it("should return an empty Map for a function without default values") {
        val supportSpy = spy(new TestFunctionSupport)
        val symb = supportSpy.getSymbol(TestMethodName).get
        val expected = Some(Map())

        supportSpy.defArgDefaults(symb, TestMethodName) should be (expected)
      }

      it("should return None if an error occurs") {
        val supportSpy = spy(new TestFunctionSupport)
        val symb = supportSpy.getSymbol(TestMethodName).get
        val expected = None

        supportSpy.defArgDefaults(symb, "DOESNTEXIST") should be (expected)
      }
    }

    describe("#valArgTypes") {
      it("should return names and types of a val function") {
        val supportSpy = spy(new TestFunctionSupport)
        doReturn(interpreter).when(supportSpy).kernelInterpreter

        val lst = List(("a", typeOf[Int]))
        val expected = Some(lst)
        supportSpy.valArgTypes(TestValName) should be (expected)
      }
    }

    describe("#valArgNames") {
      it("should return names of arguments for a val function") {
        val supportSpy = spy(new TestFunctionSupport)
        doReturn(interpreter).when(supportSpy).kernelInterpreter

        supportSpy.valArgNames(TestValName, 1).get should be (List("a"))
      }
    }

    describe("#_valArgTypes") {
      it("should return types of arguments for a val function") {
        val supportSpy = spy(new TestFunctionSupport)
        doReturn(interpreter).when(supportSpy).kernelInterpreter

        supportSpy._valArgTypes(TestValName).get should be (List(typeOf[Int]))
      }
    }

    describe("#requestReadClassInstance") {
      it("should return the instance for the code request containing the method name"){
        val supportSpy = spy(new TestFunctionSupport)

        supportSpy.requestReadClassInstance(TestMethodName).get
          .getClass.getName.contains("$read") should be (true)
      }

      it("should return none for a bad method name"){
        val supportSpy = spy(new TestFunctionSupport)

        supportSpy.requestReadClassInstance("DNE") should be (None)
      }
    }

    describe("#evalMethod") {
      it("should evaluate the method with the given name and args") {
        val supportSpy = spy(new TestFunctionSupport)

        val inst = supportSpy.requestReadClassInstance(TestMethodName).get

        supportSpy.evalMethod(inst, TestMethodName, supportSpy.toAnyRef(1)) should
          be (1)
      }
    }

    describe("#jsType") {
      it("should return JavaScript name of Int") {
        val supportSpy = spy(new TestFunctionSupport)
        supportSpy.jsType(typeOf[Int]) should be("Number")
        supportSpy.jsType(supportSpy.iMain.global.typeOf[Int].asInstanceOf[Type]) should be("Number")
      }
      it("should return JavaScript name of JsArray") {
        val supportSpy = spy(new TestFunctionSupport)
        supportSpy.jsType(typeOf[JsArray]) should be("Array")
        supportSpy.jsType(supportSpy.iMain.global.typeOf[JsArray].asInstanceOf[Type]) should be("Array")
      }
      it("should return JavaScript name of JsObject") {
        val supportSpy = spy(new TestFunctionSupport)
        supportSpy.jsType(typeOf[JsArray]) should be("Array")
        supportSpy.jsType(supportSpy.iMain.global.typeOf[JsArray].asInstanceOf[Type]) should be("Array")
      }
    }

    describe("#tryOptionFunction") {
      it("should return the underlying option data when execution returns Some") {
        val support = new TestFunctionSupport
        val f = () => Some(1)
        support.tryOptionFunction(f, "") should be (Success(1))
      }

      it("should return failure when the function returns None") {
        val support = new TestFunctionSupport
        val f = () => None
        support.tryOptionFunction(f, "").isFailure should be(true)
      }

      it("should return failure when the execution fails") {
        val support = new TestFunctionSupport
        val f = () => throw new Exception
        support.tryOptionFunction(f, "").isFailure should be(true)
      }
    }
  }
}