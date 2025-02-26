/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.implementations.imports

import implementations.AddImportStatement
import tests.util.TestHelper
import common.Change
import org.junit.Assert._
import scala.tools.refactoring.common.TextChange
import language.reflectiveCalls
import scala.tools.refactoring.util.UniqueNames

class AddImportStatementTest extends TestHelper {
  outer =>

  def addImport(imp: (String, String), src: String, expected: String) = global.ask { () =>

    val refactoring = new AddImportStatement  {
      val global = outer.global
      val file = addToCompiler(UniqueNames.basename(), src)
      val change = addImport(file, imp._1 + "." + imp._2)
    }

    assertEquals(expected, Change.applyChanges(refactoring.change, src))
  }

  @Test
  def importPackageContainsKeyword() = {
    addImport(("whatever.type", "Bla"), """
      object Main
    """,
    """
      import whatever.`type`.Bla

      object Main
    """)
  }

  @Test
  def importAnnotationOnClassWithoutPackage() = {
    addImport(("scala.annotation.unchecked", "uncheckedStable"),
    """
      @uncheckedStable
      class T
    """,
    """import scala.annotation.unchecked.uncheckedStable
@uncheckedStable
class T
    """)
  }

  @Test
  def importAnnotationOnObjectWithoutPackage() = {
    addImport(("scala.annotation.unchecked", "uncheckedStable"),
    """
      @uncheckedStable
      object T
    """,
    """import scala.annotation.unchecked.uncheckedStable
@uncheckedStable
object T
    """)
  }

  @Test
  def importWithPackageObject() = {
    addImport(("java.util", "ArrayList"), """
      // Copyright blabla
      package object foo {
        def foo(xs: ArrayList[String]): ArrayList[String] = xs
      }
    """,
    """
      // Copyright blabla
      import java.util.ArrayList
      package object foo {
        def foo(xs: ArrayList[String]): ArrayList[String] = xs
      }
    """)
  }

  @Test
  def importWithPackageObjectAndExistingImport() = {
    addImport(("java.util", "ArrayList"), """
      import java.util.Arrays
      package object foo {
        def foo(xs: ArrayList[String]): ArrayList[String] = xs
      }
    """,
    """
      import java.util.Arrays
      import java.util.ArrayList
      package object foo {
        def foo(xs: ArrayList[String]): ArrayList[String] = xs
      }
    """)
  }

  @Test
  def importInEmpty() = {
    addImport(("collection.mutable", "ListBuffer"), """
      object Main {val lb = ListBuffer(1)}
    """,
    """
      import collection.mutable.ListBuffer

      object Main {val lb = ListBuffer(1)}
    """)
  }

  @Test
  def importInEmptyWithPackage() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package xy

      object Main {val lb = ListBuffer(1)}
    """,
    """
      package xy

      import collection.mutable.ListBuffer

      object Main {val lb = ListBuffer(1)}
    """)
  }

  @Test
  def importAlreadyExisting() = {
    addImport(("collection.mutable", "ListBuffer"), """
      import collection.mutable.ListBuffer
      object Main {}
    """,
    """
      import collection.mutable.ListBuffer
      import collection.mutable.ListBuffer
      object Main {}
    """)
  }

  @Test
  def importIsInsertedAtEnd() = {
    addImport(("collection.mutable", "ListBuffer"), """
      import collection.mutable.HashMap

      import collection.mutable.HashMap

      object Main {}
    """,
    """
      import collection.mutable.HashMap
      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      object Main {}
    """)
  }

  @Test
  def importWithNestedPackages() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package nstd
      package pckg

      import collection.mutable.HashMap

      import collection.mutable.HashMap

      object Main {}
    """,
    """
      package nstd
      package pckg

      import collection.mutable.HashMap
      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      object Main {}
    """)
  }

  @Test
  def importExistsBetweenPackages() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package nstd

      import collection.mutable.HashMap

      package pckg

      import collection.mutable.HashMap
      import collection.mutable.HashMap

      object Main {}
    """, """
      package nstd

      import collection.mutable.HashMap

      import collection.mutable.HashMap
      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      package pckg

      object Main {}
    """)
  }

  @Test
  def importWithPackage() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package just.some.pkg

      object Main {val lb = ListBuffer(1)}
    """,
    """
      package just.some.pkg

      import collection.mutable.ListBuffer

      object Main {val lb = ListBuffer(1)}
    """)
  }

  @Test
  def importWithMultiplePackages() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package just
      package some
      package pkg

      object Main {val lb = ListBuffer(1)}
    """,
    """
      package just
      package some
      package pkg

      import collection.mutable.ListBuffer

      object Main {val lb = ListBuffer(1)}
    """)
  }

  @Test
  def importWithMultiplePackagesAndBraces() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package just
      package some
      package pkg {

      object Main {val lb = ListBuffer(1)}

      }
    """,
    """
      package just
      package some
      package pkg {

      import collection.mutable.ListBuffer

      object Main {val lb = ListBuffer(1)}

      }
    """)
  }

  @Test
  def importWithNestedImports() = {
    addImport(("collection.mutable", "ListBuffer"), """
      package just
      package some
      package pkg1 {

      object Main {val lb = ListBuffer(1)}

      }
      package pkg2 {

      object Main { }

      }
    """,
    """
      package just
      package some

      import collection.mutable.ListBuffer
      package pkg1 {

      object Main {val lb = ListBuffer(1)}

      }
      package pkg2 {

      object Main { }

      }
    """)
  }


  /*
   * See Assembla ticket #1002088
   */
  @Test
  def addImportWithRoundBracketStringTicket1002088Ex1() = {
    addImport(("scala.util.matching", "Regex"), """
      object X

      class Y {

        ")"

        val r: Regex
      }
      """,
      """
      import scala.util.matching.Regex

      object X

      class Y {

        ")"

        val r: Regex
      }
      """)
  }

  /*
   * This test inspired from ticket #1002088 deals with code that
   * does not compile, and therefore for now is not of very high
   * priority.
   */
  @Ignore
  @Test
  def addImportWithRoundBracketStringTicket1002088Ex2() = {
    addImport(("scala.util.matching", "Regex"), """
      object X

      ")"

      class Y {

        val r: Regex
      }
      """,
      """
      import scala.util.matching.Regex

      object X

      ")"

      class Y {

        val r: Regex
      }
      """)
  }

  @Test
  def addImportWithRoundBracketStringTicket1002088Ex3() = {
    addImport(("scala.collection.mutable", "LinkedHashSet"), """
      object DummyObject

      class WrongFormatting {
        def importHere = LinkedHashSet("(")
        ()
      }
      """,
      """
      import scala.collection.mutable.LinkedHashSet

      object DummyObject

      class WrongFormatting {
        def importHere = LinkedHashSet("(")
        ()
      }
      """)
  }

  /*
   * See Assembla Ticket #1002399
   */
  @Test
  def importIsInsertedInWrongPackage1002399() = {
    addImport(("a.b.c.actions", "ActionX"), """
    package a.b.c

    package actions {
      class ActionX
    }

    class X {
      val a: ActionX = null
    }
    """,
    """
    package a.b.c

    import a.b.c.actions.ActionX

    package actions {
      class ActionX
    }

    class X {
      val a: ActionX = null
    }
    """)
  }
}
