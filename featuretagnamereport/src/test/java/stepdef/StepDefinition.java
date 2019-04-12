package stepdef;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.BeforeStep;
import cucumber.api.java.en.And;
import io.cucumber.datatable.DataTable;

public class StepDefinition {
	
	private String scenDesc;
	
	@Before
	public void before(Scenario scenario) {
		this.scenDesc = scenario.getName();
	}
	
	@BeforeStep
	public void beforeStep() throws InterruptedException {
		Thread.sleep(100);
	}
	
	@And("this is {string} step")
	public void step(String stepNum) {
		System.out.format("Thread %2d -> %18s - %-6s STEP\n", Thread.currentThread().getId(), scenDesc, stepNum);
	}
	
	@And("these are steps")
	public void theseSteps(DataTable data) {
		System.out.println(data);
	}
}