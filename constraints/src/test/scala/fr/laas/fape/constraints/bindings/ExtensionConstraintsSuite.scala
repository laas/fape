package fr.laas.fape.constraints.bindings

import org.scalatest.FunSuite

class ExtensionConstraintsSuite extends FunSuite {

  /*
  for(cn <- List(new BindingConstraintNetwork[String](), new ConservativeConstraintNetwork[String]())) {

    test("["+cn.getClass.getSimpleName+"] Propagation of extension constraints") {
      val letters = List("A", "B", "C", "D")
      val numbers = 0 until 5

      for (letter <- letters; i <- 0 until 5)
        cn.addPossibleValue(letter + i.toString)

      for (letter <- letters)
        cn.AddVariable(letter, (0 until 5).map(i => letter + i.toString), "object")

      cn.addValuesToValuesSet("myConst", List("A1", "B1", "C1"))
      cn.addValuesSetConstraint(List("A", "B", "C"), "myConst")

      cn.addValuesToValuesSet("second", List("B1", "D1"))
      cn.addValuesToValuesSet("second", List("B1", "D2"))
      cn.addValuesToValuesSet("second", List("B2", "D3"))
      cn.addValuesSetConstraint(List("B", "D"), "second")

      assert(cn.isConsistent)

      //  cn.restrictDomain("A", List("A1"))
      cn.restrictDomain("D", List("D3"))

      assert(!cn.isConsistent)
    }


    test("["+cn.getClass.getSimpleName+"] Propagation of extension constraints (with integers)") {
      val cn = new ConservativeConstraintNetwork[String]()

      val letters = List("A", "B", "C")
      val numbers = 0 until 5

      for (letter <- letters; i <- 0 until 5)
        cn.addPossibleValue(letter + i.toString)

      for (letter <- letters)
        cn.AddVariable(letter, (0 until 5).map(i => letter + i.toString), "object")

      val iDomain = new java.util.LinkedList[Integer]
      for (i <- 1 until 5)
        iDomain.add(i)

      cn.addPossibleValue(7)
      cn.AddIntVariable("I", iDomain)

      cn.addValuesToValuesSet("myConst", List("A1", "B1", "C1"))
      cn.addValuesToValuesSet("myConst", List("A1", "B2", "C2"))
      cn.addValuesSetConstraint(List("A", "B", "C"), "myConst")

      cn.addValuesToValuesSet("duration", List("A1", "B1"), 4)
      cn.addValuesToValuesSet("duration", List("A1", "B2"), 7)
      cn.addValuesSetConstraint(List("A", "B", "I"), "duration")

      cn.restrictDomain("A", List("A1"))

      assert(cn.isConsistent)

      cn.restrictDomain("B", List("B2"))


      // false because 7 is not a valid value for I, hence B2 is not good for B
      assert(!cn.isConsistent)
    }
  }
  */
}
