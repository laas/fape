package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stnu.nilsson.EDG

class RulesSuite extends FunSuite{

  val A = 0
  val B = 1
  val C = 2
  val D = 3

  val x = 14
  val y = 11
  val u = 24
  val v = 50

  test("D1 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addRequirement(A, B, 10)
    stnu.addContingent(B, C, -4)
    stnu.addContingent(C, B, 6)

    val focus = stnu.outPosReq(A).head

    val out = stnu.D1(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == A)
    assert(added.v == C)
    assert(added.l.cond)
    assert(added.l.node == B)
    assert(added.l.value == 10 - 6)
  }

   test("D2 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addConditional(C, A, B, -y)
    stnu.addContingent(C, D, -u)
    stnu.addContingent(D, C, v)

    val focus = stnu.outConditionals(C).head

    val out = stnu.D2(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == D)
    assert(added.v == A)
    assert(added.l.cond)
    assert(added.l.node == B)
    assert(added.l.value == u-y)
  }

  test("D3 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addConditional(C, A, B, -y)
    stnu.addRequirement(D, C, v)

    val focus = stnu.outConditionals(C).head

    val out = stnu.D3(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == D)
    assert(added.v == A)
    assert(added.l.cond)
    assert(added.l.node == B)
    assert(added.l.value == v-y)
  }

  test("D4 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addRequirement(A, B, v)
    stnu.addRequirement(B, C, -x)

    val focus = stnu.outRequirements(A).head

    val out = stnu.D4(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == A)
    assert(added.v == C)
    assert(added.l.req)
    assert(added.l.value == v-x)
  }

  test("D5 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addRequirement(A, B, v)
    stnu.addConditional(B, C, D, -x)

    val focus = stnu.outRequirements(A).head

    val out = stnu.D5(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == A)
    assert(added.v == C)
    assert(added.l.cond)
    assert(added.l.node == D)
    assert(added.l.value == v-x)
  }

  test("D6 rule") {
    val stnu = new EDG(checkCycles = true)

    val x = 11
    val y = 14
    val u = 24
    val v = 50

    stnu.addRequirement(B, A, -u)
    stnu.addContingent(B, C, -x)
    stnu.addContingent(C, B, y)

    val focus = stnu.inNegReq(A).head

    val out = stnu.D6(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == C)
    assert(added.v == A)
    assert(added.l.req)
    assert(added.l.value == x-u)
  }

  test("D7 rule") {
    val stnu = new EDG(checkCycles = true)

    stnu.addRequirement(B, A, -u)
    stnu.addRequirement(C, B, y)

    val focus = stnu.inNegReq(A).head

    val out = stnu.D7(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == C)
    assert(added.v == A)
    assert(added.l.req)
    assert(added.l.value == y-u)
  }

  test("D8 rule") {
    val stnu = new EDG(checkCycles = true)

    val u = 10
    val x = 12

    stnu.addContingent(B, A, -x)
    stnu.addConditional(C, A, B, -u)

    val focus = stnu.inConditionals(A).head

    val out = stnu.D8(focus)
    assert(out._1.nonEmpty)
    assert(out._2.nonEmpty)
    val added = out._1.head

    assert(added.u == C)
    assert(added.v == A)
    assert(added.l.req)
    assert(added.l.value == -u)

    val removed = out._2.head
    assert(removed == focus)
  }

  test("D9 rule") {
    val stnu = new EDG(checkCycles = true)

    val u = 10
    val x = 8

    stnu.addContingent(B, A, -x)
    stnu.addConditional(C, A, B, -u)

    val focus = stnu.inConditionals(A).head

    val out = stnu.D9(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == C)
    assert(added.v == A)
    assert(added.l.req)
    assert(added.l.value == -x)
  }

  test("Precede reduction 1") {
    val stnu = new EDG(checkCycles = true)

    val A = 0
    val B = 1
    val C = 2
    val x = 5
    val y = 10
    val u = 15
    val v = 20

    stnu.addContingent(A, B, y)
    stnu.addContingent(B, A, -x)
    stnu.addRequirement(C, B, v)
    stnu.addRequirement(B, C, -u)

    val focus = stnu.outNegReq(B).tail.head
    assert(focus.u == B && focus.v == C && focus.l.value == -u)

    val out = stnu.PR1(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    assert(added.u == A)
    assert(added.v == C)
    assert(added.l.req)
    assert(added.l.value == x-u)
  }

  test("Precede reduction 2") {
    val stnu = new EDG(checkCycles = true)

    val A = 0
    val B = 1
    val C = 2
    val x = 5
    val y = 10
    val u = 15
    val v = 20

    stnu.addContingent(A, B, y)
    stnu.addContingent(B, A, -x)
    stnu.addRequirement(C, B, v)
    stnu.addRequirement(B, C, -u)

    val focus = stnu.inPosReq(B).tail.head
    assert(focus.u == C && focus.v == B && focus.l.value == v)

    val out = stnu.PR2(focus)
    assert(out._1.nonEmpty)
    assert(out._2.isEmpty)
    val added = out._1.head

    println(added)
    assert(added.u == C)
    assert(added.v == A)
    assert(added.l.req)
    assert(added.l.value == v-y)
  }
}
