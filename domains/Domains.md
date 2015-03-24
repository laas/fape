---
title: Domains
...


# HandOver (RIS)

This domain defines areas and robots attached to them.
The problem defines a set of areas, each robots starts within an area and cannot get out of it.
Areas might be connected (e.g. there is a window between the two) in which case two robots in those areas can exchange objects.
The goals is to find a feasible plan to move objects from one point to another. The available actions are

**Actions:**

 - pick
 - place
 - handover (give an object to another robot in a connected area)
 - move (moves a robot within its area)

The main difficulty of this problem comes from reachability: a lot of actions will not be doable due to constraints on the robots position. This is hard to find out for a plan-space planner and leads to frequent backtracks.


**Properties:**

 - multiple agents
 - synchronization
 - subplans (for different subgoals) can be made independently but good plans (in terms of makespan) will typically have a lot of interdependencies


# Docks (APTP)

Logistics problem where containers should be moved between locations. It is an extension of the blocks world problem : container are stored as stacks to move a container, there should be no container on top of it.
On top of that, stacks are associated with areas containing one or multiple cranes. Containers can be move between stacks of the same area using cranes. Robots/Trucks can be used to transport containers between areas, cranes being used to load/unload trucks.
At any time, there should me at most one truck in an area. This yields some strongly concurrent actions when two robots have to swap locations.

**Properties:**

 - multiple agents
 - goals are highly interdependent (similar to Sussman anomaly)

# Rovers (IPC)

Planetary exploration problem where a fleet of rovers have to collect rock samples and take images of remote locations. Those images and the analizes of the samples should then be communicated back to the lander. Constraints for communication are only visibility, a communication should only be done from a place where the lander is visible.

**Properties:**

 - multiple agents
 - navigation


## Extension

A desirable extension is to add temporal constraints on the communication action. Instead of communicating with the lander do it with an orbiting satellite to allow definition of visibility windows.


# Brackets (Saphari european project)

Describes an industrial process where a sequence of actions (Clean, Glue, Attach) have to be repeated for a set of pieces.
A problem has a number of human operators that are qualified to perform the process and a number of robots to help them.

Performing the process has some requirements : some objects must be available to the human operator and the surface should be lightened when the surface is glued.
Objects can be moved around and exchanged by both humans and robots. Human operators only can perform the clean-attach-glue process. Lightening the surface (`point` action) can only be done by robots.

This separation of tasks creates the need for joint and strongly concurrent actions: the `glue` action can only be done within a `point`. `glue` has an uncontrollable duration while `point` has a controllable duration but must end after the contained `glue`.
A similar setting is used for the `handover` action where a robot proposes an object to the human operator until this one decides to take it.


**Properties:**

 - well defined procedure, planning freedom lies in task inserted to bring objects to the operators
 - multiple agents
 - joint actions, synchronization
 - some actions cannot be undone


# International Travel

This is a hierarchical domain for traveling.
Displacement actions are spread on three hierarchical levels:

1) Countries: `InternationalMove` & `NationalMove`, updates the country the traveler is in
2) Cities: `Move`, updates the city the traveler is in
3) Transportation modes: `MoveByBus`, `MoveByTrain`, `MoveByPlane` updates the time needed for a move action.

Those levels are highly inter-dependent: an action in one level must match (compatible parameters and same start/end timepoints) exactly one action in every other level.

A few other actions can be inserted for fulfill conditions of the displacement actions. Those include getting a visa with some wait constraints, and renting/returning a car with constraints on the location the car must be returned to.

**Properties:**

 - single agent
 - highly hierarchical
 


## Improvements

Durations and feasibility are defined at level 3) only. This defeats the purpose of the domain: we have no idea whether a plan at level 1) or 2) is feasible until we have completly decomposed it.

Deducing reachability and duration bounds for higher level should be easy to do through automated reasonning.



# Hide From (Rachid)

This is just a toy problem where the goal is to hide an object from another agent.
This is done by using conditionnal effects on the pick and place actions. The belief of the other agent is updated depending on its location.
The goal is expressed as a condition on the belief state of the other agent.




# TMS (stands for?)

This a scheduling problem for baking pieces. Given a set of structures to make, one must select a hoven to bake the two pieces and select a third hoven to bake the resulting structures.


# Depth


# Survivors (Onera)


# Arm

