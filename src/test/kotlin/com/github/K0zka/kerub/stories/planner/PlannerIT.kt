package com.github.K0zka.kerub.stories.planner

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

RunWith(Cucumber::class)
CucumberOptions(
		plugin = arrayOf("pretty"),
		features = arrayOf("classpath:stories/general/planner/planner.feature",
		                   "classpath:stories/general/planner/power-save.feature"
		                  ),
		glue = arrayOf("com.github.K0zka.kerub.stories.planner")
               )

public class PlannerIT {
}