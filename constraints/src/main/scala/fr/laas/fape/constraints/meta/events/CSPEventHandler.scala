package fr.laas.fape.constraints.meta.events

import fr.laas.fape.constraints.meta.CSP

trait CSPEventHandler {

  def handleEvent(event: Event)

  def clone(newCSP: CSP) : CSPEventHandler

}
