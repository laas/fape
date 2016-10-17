package fr.laas.fape.constraints.bindings

import org.scalatest.FunSuite

class BindingsSuite extends FunSuite {

  /*

  for(cn <- List(new BindingConstraintNetwork[String](), new ConservativeConstraintNetwork[String]())) {

    test("aeaze"+ cn.getClass.getSimpleName) {
      cn.addPossibleValue("a")
      cn.addPossibleValue("b")
      cn.addPossibleValue("c")

      val letters = List("A", "B", "C", "D")
      val numbers = 0 until 5

      for (letter <- letters; i <- 0 until 5)
        cn.addPossibleValue(letter + i.toString)

      for (letter <- letters)
        cn.AddVariable(letter, (0 until 5).map(i => letter + i.toString), "object")

      cn.AddVariable("Abis", (0 until 3).map(i => "A" + i.toString), "object")

      println(cn.Report())

      //    cn.AddUnificationConstraint("A","Abis")
      cn.AddSeparationConstraint("A", "Abis")
      println(cn.Report())

      cn.restrictDomain("A", List("A2"))
      cn.isConsistent

      println(cn.getUnboundVariables)
      println(cn.Report())
    }
  }
  */
}
