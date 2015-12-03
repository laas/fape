package planstack.constraints

import planstack.graph.core.LabeledEdge

package object stnu {
  /**
   * Edge type in an EDG.
   */
  type Edge[ID] = LabeledEdge[Int, STNULabel[ID]]
}
