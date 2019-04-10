package formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestRunFinished;

public class DryRunFormatter implements EventListener {

	private Map<String, List<String>> tagToScenarioMap = new TreeMap<>();
	private Map<String, String> scenarioToNameMap = new TreeMap<>();
	
	
	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestCaseStarted.class, this::handleCaseStarted);
		publisher.registerHandlerFor(TestRunFinished.class, this::handleRunFinished);
	}

	private void handleCaseStarted(TestCaseStarted event) {
		scenarioToNameMap.put(event.getTestCase().getUri() + " # " + event.getTestCase().getLine(), event.getTestCase().getName());
		
		event.getTestCase().getTags().stream().forEach(t -> {
			List<String> scen = new ArrayList<>();
			scen.add(event.getTestCase().getUri() + " # " + event.getTestCase().getLine());
			tagToScenarioMap.merge(t.getName(), scen, (l1, l2) -> {
				l1.addAll(l2);
				return l1;
			});
		});
	}
	
	private void handleRunFinished(TestRunFinished event) {
		System.out.println("---------------------");
		tagToScenarioMap.forEach((k, v) -> {
			System.out.println(k);
			v.forEach(l -> System.out.println("\t" + l));
		});
		System.out.println("---------------------");
		scenarioToNameMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v);
		});
	}

}
