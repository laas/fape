package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stnu.nilsson.CCGraph

class CCGraphSuite extends FunSuite {

  test("No cycle") {
    val cc = new CCGraph
    cc.addEdge(0, 1)
    cc.addEdge(1, 2)
    cc.addEdge(4, 5)
    cc.addEdge(5, 192)
    assert(cc.acyclic)
  }

  test("Simple cycle") {
    val cc = new CCGraph
    cc.addEdge(0, 1)
    cc.addEdge(1, 2)
    cc.addEdge(2, 1)
    cc.addEdge(5, 192)
    assert(!cc.acyclic)
  }

}
