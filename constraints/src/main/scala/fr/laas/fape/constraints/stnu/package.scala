package fr.laas.fape.constraints

import fr.laas.fape.constraints.stnu.nilsson.STNULabel
import planstack.graph.core.LabeledEdge

package object stnu {
  /**
   * Edge type in an EDG.
   */
  type Edge[ID] = LabeledEdge[Int, STNULabel[ID]]
}
