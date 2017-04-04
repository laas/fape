package fr.laas.fape.constraints.meta.events

trait IEventHandler {

  def handleEvent(event: Event)

}
