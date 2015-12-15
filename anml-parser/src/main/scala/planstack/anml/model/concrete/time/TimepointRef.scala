package planstack.anml.model.concrete.time

import planstack.anml.model._
import planstack.anml.model.abs.time._
import planstack.anml.model.concrete.{RefCounter, TPRef}


object TimepointRef {

  def apply(pb:AnmlProblem, context:Context, abs:AbsTP, refCounter: RefCounter) : TPRef = {
    abs match {
      case TimeOrigin => pb.start
      case ContainerStart => context.interval.start
      case ContainerEnd => context.interval.end
      case IntervalStart(id) => context.getIntervalWithID(id).start
      case IntervalEnd(id) => context.getIntervalWithID(id).end
      case StandaloneTP(name) => context.getTimepoint(name, refCounter)
    }
  }
}
