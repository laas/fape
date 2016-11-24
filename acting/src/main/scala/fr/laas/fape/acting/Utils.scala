package fr.laas.fape.acting

import com.sun.org.apache.xpath.internal.operations.VariableSafeAbsRef
import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.model.concrete.statements.Persistence
import fr.laas.fape.planning.core.planning.states.State
import fr.laas.fape.planning.core.planning.states.modification.ChronicleInsertion

/**
  * Created by abitmonn on 11/23/16.
  */
object Utils {

  private var problem : AnmlProblem = null

  def setProblem(file: String): Unit = {
    problem = new AnmlProblem()
    problem.extendWithAnmlFile(file)
  }

  def getProblem = {
    require(problem != null)
    problem
  }

  def buildGoal(svName: String, args: List[String], value: String, deadline: Int = -1) = {
    assert(RefCounter.useGlobalCounter)
    val goal = new Chronicle
    val statement = new Persistence(
      problem.stateVariable(svName, args),
      problem.instance("true"),
      goal,
      RefCounter.getGlobalCounter)
    goal.addStatement(statement)
    if(deadline > -1) {
      goal.addConstraint(new MinDelayConstraint(statement.start, problem.start, -deadline))
    }
    new ChronicleInsertion(goal)
  }

  def buildTask(name: String, args: List[String], deadline: Int = -1) = {
    assert(RefCounter.useGlobalCounter)
    val goal = new Chronicle
    val task = new Task("t-"+name, args.map(problem.instance(_)), None, problem.refCounter)
    goal.addTask(task)
    if(deadline > -1) {
      goal.addConstraint(new MinDelayConstraint(task.end, problem.start, -deadline))
    }
    new ChronicleInsertion(goal)
  }

  def asString(variable: VarRef, plan: State) = {
    plan.domainOf(variable).get(0)
  }
}
