##################
# Reserved words #
################################################################
# HybridHTNDomain                                              #
# MaxArgs                                                      #
# :operator                                                    #
# :method                                                      #
# Head                                                         #
# Pre                                                          #
# Add                                                          #
# Del                                                          #
# Sub                                                          #
# Constraint                                                   #
# Ordering                                                     #
# Type                                                         #
# Values                                                       #
# StateVariable                                                #
# FluentResourceUsage                                          #
# Param                                                        #
# ResourceUsage                                                #
# Usage                                                        #
# Resource                                                     #
# Fluent                                                       #
#                                                              #
##   All AllenIntervalConstraint types                         #
##   '[' and ']' should be used only for constraint bounds     #
##   '(' and ')' are used for parsing                          #
#                                                              #
################################################################

(HybridHTNDomain DWRDomain)

(MaxArgs 5)

(PredicateSymbols
# static relations
  adjacent # adjacent(dock waypoint)
  connected
  
  
# predicates
  r_loc       # robot is at location
  d_occupant  # d_occupant(dock robot)
  k_attached  # k_attached(crane dock)
  p_ondock    # p_ondock(pile dock)
  p_available # p_available(pile true/false)
  k_grip      # k_grip(crane container)
  c_in        # c_in(container pile)
  c_on        # c_on(container container)
  p_top       # p_top(pile container)
  r_freight   # r_freight(robot container)

# Operators:
  !leave      # !leave(robot dock waypoint) leaves a dock to a waypoint
  !enter      # !enter(robot dock waypoint) enters a dock from a waypoint
  !move       # !move(robot waypoint waypoint)
  !stack      # ?crane holding ?container stacks it on top of ?pile
  !unstack    # empty ?crane unstacks ?container from ?pile
  !put        # ?crane holding ?conrainer puts it on ?robot which was empty
  !take       # empty ?crane takes ?conrainer from ?robot

# Methods
  load        # load ?container from ?pile onto ?robot
  unload      # unload ?container from ?robot onto ?pile
  uncover     # ?container in ?pile is rearranged onto the top of ?pile
  navigate    # ?robot navigates between two waypoints
  goto        # ?robot goes to ?dock
  bring       # bring ?container to ?pile
  robot_bring # robot brings container to pile
)

(StateVariable r_loc 1 r1 r2)
(StateVariable d_occupant 1 d1 d2 d3 d4)
(StateVariable k_attached 1 k1 k2 k3 k4)
(StateVariable p_ondock 1 p11 p12 p21 p22 p3 p4)
(StateVariable p_available 1 p11 p12 p21 p22 p3 p4)
(StateVariable k_grip 1 k1 k2 k3 k4)
(StateVariable c_in 1 c11 c12 c21 c22)
(StateVariable c_on 1 c11 c12 c21 c22)
(StateVariable p_top 1 p11 p12 p21 p22 p3 p4)
(StateVariable r_freight 1 r1 r2)

# TODO DEFINE RESOURCES

################################
####  OPERATORS ################

# leave a dock
(:operator
 (Head !leave(?robot ?dock ?waypoint))
 (Constraint Duration[10000,INF](task))
 (Pre p0 adjacent(?dock ?waypoint))
 (Constraint During(task,p0))
 (Pre p1 r_loc(?robot ?dock))
 (Del p1)
 (Pre p2 d_occupant(?dock ?robot))
 (Del p2)
 (Add e1 r_loc(?robot ?waypoint))
 (Constraint Meets(p1,e1))
 (Add e2 d_occupant(?dock ?free))
 (Constraint Meets(p2,e2))
 (Values ?free free)
)

# enter a dock
(:operator
 (Head !enter(?robot ?dock ?waypoint))
 (Constraint Duration[20000,INF](task))
 (Pre p0 adjacent(?dock ?waypoint))
 (Constraint During(task,p0))
 (Pre p1 r_loc(?robot ?waypoint))
 (Del p1)
 (Pre p2 d_occupant(?dock ?free))
 (Del p2)
 (Add e1 r_loc(?robot ?dock))
 (Constraint Meets(p1,e1))
 (Add e2 d_occupant(?dock ?robot))
 (Constraint Meets(p2,e2))
 (Values ?free free)
)

# move from waypoint1 to waypoint2
(:operator
 (Head !move(?robot ?wp1 ?wp2))
 (Constraint Duration[10000,20000](task))
 (Pre p0 connected(?wp1 ?wp2))
 (Constraint During(task,p0))
 (Pre p1 r_loc(?robot ?wp1))
 (Del p1)
 (Add e1 r_loc(?robot ?wp2))
 (Constraint Meets(p1,e1))
)

# ?crane holding ?container stacks it on top of ?pile
(:operator
 (Head !stack(?crane ?container ?pile))
 (Constraint Duration[10000,INF](task))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 p_available(?pile ?true)) # ASK: Why should the pile be available???
 (Values ?true true)
 (Pre p3 k_grip(?crane ?container))
 (Del p3)
 (Add e3 k_grip(?crane ?empty))
 (Values ?empty empty)
 (Constraint Meets(p3,e3))
 (Pre p4 c_in(?container ?crane))
 (Del p4)
 (Add e4 c_in(?container ?pile))
 (Constraint Meets(p4,e4)) 
 (Pre p5 c_on(?container ?empty))
 (Del p5)
 (Add e5 c_on(?container ?prevtop))
 (Constraint Meets(p5,e5))
 (Pre p6 p_top(?pile ?prevtop))
 (Del p6)
 (Add e6 p_top(?pile ?container))
 (Constraint Meets(p6,e6))
)

# empty ?crane unstacks ?container from ?pile
(:operator
 (Head !unstack(?crane ?container ?pile))
 (Constraint Duration[10000,INF](task)) 
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 p_available(?pile ?true)) # ASK: Why should the pile be available???
 (Values ?true true)
 (Pre p3 k_grip(?crane ?empty))
 (Del p3)
 (Values ?empty empty)
 (Add e3 k_grip(?crane ?container))
 (Constraint Meets(p3,e3))
 (Pre p4 c_in(?container ?pile))
 (Del p4)
 (Add e4 c_in(?container ?crane))
 (Constraint Meets(p4,e4))
 (Pre p5 c_on(?container ?nextop))
 (Del p5)
 (Add e5 c_on(?container ?empty))
 (Constraint Meets(p5,e5))
 (Pre p6 p_top(?pile ?container))
 (Del p6)
 (Add e6 p_top(?pile ?nextop))
 (Constraint Meets(p6,e6))
)

# ?crane holding ?conrainer puts it on ?robot which was empty
(:operator
 (Head !put(?crane ?container ?robot))
 (Constraint Duration[10000,INF](task))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 k_grip(?crane ?container))
 (Del p1)
 (Add e1 k_grip(?crane ?empty))
 (Values ?empty empty)
 (Constraint Meets(p1,e1))
 (Pre p2 r_freight(?robot ?empty))
 (Del p2)
 (Add e2 r_freight(?robot ?container))
 (Constraint Meets(p2,e2))
 (Pre p3 c_in(?container ?crane))
 (Del p3)
 (Add e3 c_in(?container ?robot))
 (Constraint Meets(p3,e3))
 (Pre p4 r_loc(?robot ?dock))
 (Constraint During(task,p4))
)

# empty ?crane takes ?conrainer from ?robot
(:operator
 (Head !take(?crane ?container ?robot))
 (Constraint Duration[10000,INF](task))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 k_grip(?crane ?empty))
 (Del p1)
 (Add e1 k_grip(?crane ?container))
 (Values ?empty empty)
 (Constraint Meets(p1,e1))
 (Pre p2 r_freight(?robot ?container))
 (Del p2)
 (Add e2 r_freight(?robot ?empty))
 (Constraint Meets(p2,e2))
 (Pre p3 c_in(?container ?robot))
 (Del p3)
 (Add e3 c_in(?container ?crane))
 (Constraint Meets(p3,e3))
 (Pre p4 r_loc(?robot ?dock))
 (Constraint During(task,p4))
)


##################### METHODS #####################################

# load ?container from ?pile onto ?robot
(:method
 (Head load(?container ?robot ?pile))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 p_top(?pile ?container))
 (Pre p3 r_loc(?robot ?dock))
 (Constraint During(task,p3))
 (Pre p4 p_available(?pile ?true))
 (Values ?true true)
 (Constraint During(task,p4))
 (Sub s1 !unstack(?crane ?container ?pile))
 (Sub s2 !put(?crane ?container ?robot))
 (Constraint BeforeOrMeets(s1,s2))
 (Ordering s1 s2)
)

# unload ?container from ?robot onto ?pile
(:method
 (Head unload(?container ?robot ?pile))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 c_in(?container ?robot))
 (Pre p3 r_loc(?robot ?dock))
 (Constraint During(task,p3))
 (Pre p4 p_available(?pile ?true))
 (Values ?true true)
 (Constraint During(task,p4))
 (Sub s1 !take(?crane ?container ?robot))
 (Sub s2 !stack(?crane ?container ?pile))
 (Constraint BeforeOrMeets(s1,s2))
 (Ordering s1 s2)
)

# ?container in ?pile is rearranged onto the top of ?pile
(:method 
 (Head uncover(?container ?pile))
 (Pre p0 c_in(?container ?pile))
 (Pre p1 p_top(?pile ?container))
)

(:method 
 (Head uncover(?container ?pile))
 (Pre p0 c_in(?container ?pile))
 (Pre p1 p_top(?pile ?prevtop))
 (VarDifferent ?prevtop ?container)
 (Pre p2 k_attached(?crane ?dock))
 (Pre p3 p_ondock(?pile ?dock))
 (Pre p4 p_ondock(?otherp ?dock))
 (VarDifferent ?otherp ?pile)
 (Pre p5 p_available(?pile ?true))
 (Pre p6 p_available(?otherp ?true))
 (Values ?true true)
 (Sub s1 !unstack(?crane ?prevtop ?pile))
 (Sub s2 !stack(?crane ?prevtop ?otherp))
 (Sub s3 uncover(?container ?pile))
 (Constraint BeforeOrMeets(s1,s2))
 (Constraint BeforeOrMeets(s2,s3))
 (Ordering s1 s2)
 (Ordering s2 s3)
)

# ?robot navigates between two waypoints
(:method
 (Head navigate(?robot ?wp1 ?wp2))
 (Pre p0 connected(?wp1 ?wp2))
 (Sub s1 !move(?robot ?wp1 ?wp2))
)

(:method
 (Head navigate(?robot ?wp1 ?wp2))
 (Pre p0 connected(?wp1 ?wp3))
 (VarDifferent ?wp2 ?wp3)
 (Sub s1 !move(?robot ?wp1 ?wp3))
 (Sub s2 navigate(?robot ?wp3 ?wp2))
 (Constraint BeforeOrMeets(s1,s2))
 (Ordering s1 s2)
 (Constraint Duration[1,30000](task))
)

# ?robot goes to ?dock
(:method
 (Head goto(?robot ?dock))
 (Pre p0 r_loc(?robot ?dock))
)

(:method
 (Head goto(?robot ?dock))
 (Pre p0 r_loc(?robot ?from))
 (VarDifferent ?from ?dock)
 (Pre p1 adjacent(?from ?wp1))  # TODO USE TYPE FOR WP1 from types instances map
 (Pre p2 adjacent(?dock ?wp2))
 (Sub s1 !leave(?robot ?from ?wp1))
 (Sub s2 navigate(?robot ?wp1 ?wp2))
 (Sub s3 !enter(?robot ?dock ?wp2))
 (Constraint BeforeOrMeets(s1,s2))
 (Constraint BeforeOrMeets(s2,s3))
 (Ordering s1 s2)
 (Ordering s2 s3)
)

# bring ?container to ?pile
# already there
(:method
 (Head bring(?container ?pile))
 (Pre p0 c_in(?container ?pile))
 (Constraint During(task,p0))
 (Constraint Duration[1,1](task))
)

# alredy at correct dock
(:method
 (Head bring(?container ?pile))
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 p_ondock(?otherp ?dock))
 (VarDifferent ?pile ?otherp)
 (Pre p3 c_in(?container ?otherp))
 (Pre p4 p_available(?pile ?true))
 (Pre p5 p_available(?otherp ?true))
 (Values ?true true)
 (Sub s1 uncover(?container ?otherp))
 (Sub s2 !unstack(?crane ?container ?otherp))
 (Sub s3 !stack(?crane ?container ?pile))
 (Constraint BeforeOrMeets(s1,s2))
 (Constraint BeforeOrMeets(s2,s3))
 (Ordering s1 s2)
 (Ordering s2 s3)
)

# bring to other dock
(:method
 (Head bring(?container ?pile))
 (Type ?robot Robot)
 (Pre p0 p_ondock(?pile ?todock))
 (Pre p1 p_ondock(?otherp ?fromdock))
 (VarDifferent ?fromdock ?todock)
 (Pre p2 c_in(?container ?otherp))
 (Pre p3 p_available(?pile ?true))
 (Values ?true true)
 (Pre p4 p_available(?otherp ?true))
 (Sub s1 goto(?robot ?fromdock))
 (Sub s2 uncover(?container ?otherp))
 (Sub s3 load(?container ?robot ?otherp))
 (Sub s4 goto(?robot ?todock))
 (Sub s5 unload(?container ?robot ?pile))
 (Constraint BeforeOrMeets(s1,s3))
 (Constraint BeforeOrMeets(s2,s3))
 (Constraint BeforeOrMeets(s3,s4))
 (Constraint BeforeOrMeets(s4,s5))
 (Ordering s1 s2)
 (Ordering s2 s3)
 (Ordering s3 s4)
 (Ordering s4 s5)
)

# bring ?container to ?pile with robot
# already there
(:method
 (Head robot_bring(?robot ?container ?pile))
 (Type ?robot Robot)
 (Pre p0 c_in(?container ?pile))
 (Constraint During(task,p0))
 (Constraint Duration[1,1](task))
)

# alredy at correct dock
(:method
 (Head robot_bring(?robot ?container ?pile))
 (Type ?robot Robot)
 (Pre p0 k_attached(?crane ?dock))
 (Pre p1 p_ondock(?pile ?dock))
 (Pre p2 p_ondock(?otherp ?dock))
 (VarDifferent ?pile ?otherp)
 (Pre p3 c_in(?container ?otherp))
 (Pre p4 p_available(?pile ?true))
 (Pre p5 p_available(?otherp ?true))
 (Values ?true true)
 (Sub s1 uncover(?container ?otherp))
 (Sub s2 !unstack(?crane ?container ?otherp))
 (Sub s3 !stack(?crane ?container ?pile))
 (Constraint BeforeOrMeets(s1,s2))
 (Constraint BeforeOrMeets(s2,s3))
 (Ordering s1 s2)
 (Ordering s2 s3)
)

# bring to other dock
(:method
 (Head robot_bring(?robot ?container ?pile))
 (Type ?robot Robot)
 (Pre p0 p_ondock(?pile ?todock))
 (Pre p1 p_ondock(?otherp ?fromdock))
 (VarDifferent ?fromdock ?todock)
 (Pre p2 c_in(?container ?otherp))
 (Pre p3 p_available(?pile ?true))
 (Values ?true true)
 (Pre p4 p_available(?otherp ?true))
 (Sub s1 goto(?robot ?fromdock))
 (Sub s2 uncover(?container ?otherp))
 (Sub s3 load(?container ?robot ?otherp))
 (Sub s4 goto(?robot ?todock))
 (Sub s5 unload(?container ?robot ?pile))
 (Constraint BeforeOrMeets(s1,s3))
 (Constraint BeforeOrMeets(s2,s3))
 (Constraint BeforeOrMeets(s3,s4))
 (Constraint BeforeOrMeets(s4,s5))
 (Ordering s1 s2)
 (Ordering s2 s3)
 (Ordering s3 s4)
 (Ordering s4 s5)
)