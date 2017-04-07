package fr.laas.fape.constraints.meta.util

import java.lang.management.ManagementFactory

import scala.annotation.elidable
import scala.annotation.elidable._

/** Provides a flexible assertion support, related to a debug level.
  *
  * DEBUG_LEVEL == 1, should be suitable for a release version of the software, with inexpensive sanity checks.
  * DEBUG_LEVEL 2 and 3, encompass more expensive sanity checks that should only be made in debug mode.
  * DEBUG_LEVEL 4 is reserved to very expensive computation that should never be used except beside development.
  *
  * Default debug level is 3 when java assertions are enabled (VM option "-ea") and 1 otherwise.
  *
  * All assert methods have lazy evaluation of the all parameters, so invocation should be almost for free when above
  * the current DEBUG_LEVEL.
  *
  * Assertion can be remove at compile time with "-Xelide-below" parameter to scalac.
  */
object Assertion {

  /** By default, debug level is set to 3 if java assertions are enabled (with VM option "-ea") and 1 otherwise */
  var DEBUG_LEVEL =
    if(ManagementFactory.getRuntimeMXBean.getInputArguments.contains("-ea"))
      3
    else
      1

  @elidable(ASSERTION) @inline
  final def assert1(assertion: => Boolean, message: => Any) {
    if (DEBUG_LEVEL >= 1 && !assertion)
      throw new java.lang.AssertionError("assertion failed: "+ message)
  }

  @elidable(FINE) @inline
  final def assert2(assertion: => Boolean, message: => Any) {
    if (DEBUG_LEVEL >= 2 && !assertion)
      throw new java.lang.AssertionError("assertion failed: "+ message)
  }

  @elidable(FINER) @inline
  final def assert3(assertion: => Boolean, message: => Any) {
    if (DEBUG_LEVEL >= 3 && !assertion)
      throw new java.lang.AssertionError("assertion failed: "+ message)
  }

  @elidable(FINEST) @inline
  final def assert4(assertion: => Boolean, message: => Any) {
    if (DEBUG_LEVEL >= 4 && !assertion)
      throw new java.lang.AssertionError("assertion failed: "+ message)
  }

  @elidable(ASSERTION) @inline
  final def assert1(assertion: => Boolean) {
    if (DEBUG_LEVEL >= 1 && !assertion)
      throw new java.lang.AssertionError("assertion failed")
  }

  @elidable(FINE) @inline
  final def assert2(assertion: => Boolean) {
    if (DEBUG_LEVEL >= 2 && !assertion)
      throw new java.lang.AssertionError("assertion failed")
  }

  @elidable(FINER) @inline
  final def assert3(assertion: => Boolean) {
    if (DEBUG_LEVEL >= 3 && !assertion)
      throw new java.lang.AssertionError("assertion failed")
  }

  @elidable(FINEST) @inline
  final def assert4(assertion: => Boolean) {
    if (DEBUG_LEVEL >= 4 && !assertion)
      throw new java.lang.AssertionError("assertion failed")
  }

}
