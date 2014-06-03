package planstack.constraints

import planstack.graph.core.LabeledEdge

package object stnu {
  /**
   * Edge type in an EDG.
   */
  type E = LabeledEdge[Int, STNULabel]
}
