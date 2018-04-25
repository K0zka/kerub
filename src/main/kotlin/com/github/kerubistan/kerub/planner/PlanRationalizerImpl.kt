package com.github.kerubistan.kerub.planner

import com.github.k0zka.finder4j.backtrack.StepFactory
import com.github.kerubistan.kerub.planner.issues.problems.CompositeProblemDetectorImpl
import com.github.kerubistan.kerub.planner.issues.problems.Problem
import com.github.kerubistan.kerub.planner.issues.problems.ProblemDetector
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStep

class PlanRationalizerImpl(
		private val stepFactory: StepFactory<AbstractOperationalStep, Plan>,
		private val violationDetector: PlanViolationDetector = PlanViolationDetectorImpl,
		private val problemDetector: ProblemDetector<Problem> =  CompositeProblemDetectorImpl
) : PlanRationalizer {

	/**
	 * Logic behind the rationalization process:
	 *  - take each step
	 *  - check that the following steps could have been taken from previous
	 *  	states up to a point that is a solution itself
	 * - if that is so, remove all steps before and after
	 * - if not, continue with next step
	 */
	override fun rationalize(plan: Plan): Plan =
			if (plan.steps.size > 1) {
				(1..(plan.steps.size - 1)).map {
					subPlan(plan, it)
				}.filterNotNull().minBy { it.steps.size } ?: plan
			} else plan

	/**
	 * Create subplan
	 */
	private fun subPlan(plan: Plan, startStepIndex: Int): Plan? {

		val subSteps = plan.steps.subList(fromIndex = startStepIndex, toIndex = plan.steps.size)
		var states = listOf(plan.state)
		var validatedSteps = listOf<AbstractOperationalStep>()
		subSteps.forEach { step ->
			val steps = stepFactory.produce(Plan(states = states, steps = validatedSteps))

			if (!steps.contains(step)) {
				return null
			}

			val state = step.take(states.last())

			validatedSteps += step
			states += state

			if (isTargetState(plan)) {
				return Plan(states = states, steps = validatedSteps)
			}


		}

		return Plan(steps = validatedSteps, states = states)
	}

	private fun isTargetState(plan: Plan): Boolean =
			violationDetector.listViolations(plan).isEmpty()
					&& problemDetector.detect(plan).isEmpty()
}