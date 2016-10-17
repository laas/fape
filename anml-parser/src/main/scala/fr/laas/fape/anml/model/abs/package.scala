package fr.laas.fape.anml.model

import fr.laas.fape.anml.model.concrete.Action

/** Classes and objects in this package describe anml objects (such as actions, temporal statements, ...)
  * that are lifted and only references local variables (such as the parameters of an action, for the
  * variables appearing in an action.
  *
  * It relates to the package [[planstack.anml.model.concrete]], which represents concrete instances.
  * For instance an [[Action]] is an instance of a "class" AbstractAction that was given
  * some parameters in the form of global variables.
  */
package object abs {

}
