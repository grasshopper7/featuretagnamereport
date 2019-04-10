package cucumber.runtime.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestSourceRead;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;

public class DetailedDryRunFormatter implements EventListener {

	private TestSourcesModel tsm = new TestSourcesModel();

	private Map<String, String> featureToNameMap = new TreeMap<>();
	private Map<String, String> featureToDescMap = new TreeMap<>();
	private Map<String, ScenarioOutline> rowToScenOutMap = new TreeMap<>();
	private Map<String, ScenarioDefinition> rowToScenariosMap = new TreeMap<>();
	private Map<String, List<String>> tagToScenariosMap = new TreeMap<>();
	private Map<String, List<String>> scenarioToTagsMap = new TreeMap<>();
	private Map<String, String> scenarioToNameMap = new TreeMap<>();
	private Map<String, String> scenarioToDescMap = new TreeMap<>();

	
	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestSourceRead.class, this::handleSourceRead);
		publisher.registerHandlerFor(TestCaseStarted.class, this::handleCaseStarted);
		publisher.registerHandlerFor(TestRunFinished.class, this::handleRunFinished);
	}

	private void handleSourceRead(TestSourceRead event) {
		tsm.addTestSourceReadEvent(event.uri, event);

		Feature feature = tsm.getFeature(event.uri);

		featureToNameMap.put(event.uri, feature.getName());
		String fDesc = ((feature.getDescription() == null) ? "" : feature.getDescription());
		featureToDescMap.put(event.uri, fDesc);

		List<ScenarioDefinition> sd = feature.getChildren();

		sd.stream().forEach(sc -> rowToScenariosMap.put(event.uri + " # " + sc.getLocation().getLine(), sc));

		List<ScenarioOutline> scenout = sd.stream().filter(s -> s instanceof ScenarioOutline)
				.map(so -> (ScenarioOutline) so).collect(Collectors.toList());

		scenout.stream().forEach(so -> so.getExamples().stream().forEach(e -> e.getTableBody().stream()
				.forEach(t -> rowToScenOutMap.put(event.uri + " # " + t.getLocation().getLine(), so))));
	}

	private void handleRunFinished(TestRunFinished event) {

		System.out.println("---Feature Name-------------");
		featureToNameMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v);
		});
		System.out.println("---Feature Desc-------------");
		featureToDescMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v);
		});
		System.out.println("---Scenario Name------------");
		scenarioToNameMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v);
		});
		System.out.println("---Scenario Desc------------");
		scenarioToDescMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v);
		});
		System.out.println("---Tags to Scenario---------");
		tagToScenariosMap.forEach((k, v) -> {
			System.out.println(k);
			v.forEach(l -> System.out.println("\t" + l));
		});
		System.out.println("---Scenarios to Tag---------");
		tagToScenariosMap.keySet().stream().forEach(k -> tagToScenariosMap.get(k).forEach(v -> {
			List<String> key = new ArrayList<>();
			key.add(k);
			scenarioToTagsMap.merge(v, key, (l1, l2) -> {
				l1.addAll(l2);
				return l1;
			});
		}));
		scenarioToTagsMap.forEach((k, v) -> {
			System.out.println(k);
			v.forEach(l -> System.out.println("\t" + l));
		});

	}

	private void handleCaseStarted(TestCaseStarted event) {

		String key = event.getTestCase().getUri() + " # " + event.getTestCase().getLine();

		event.getTestCase().getTags().stream().forEach(t -> {
			List<String> scen = new ArrayList<>();

			if (rowToScenOutMap.containsKey(key))
				scen.add(event.getTestCase().getUri() + " # " + rowToScenOutMap.get(key).getLocation().getLine());
			else
				scen.add(key);

			tagToScenariosMap.merge(t.getName(), scen, (l1, l2) -> {
				if (!tagToScenariosMap.get(t.getName()).contains(l2.get(0)))
					l1.addAll(l2);
				return l1;
			});
		});

		String soKey = key;
		if (rowToScenOutMap.containsKey(key) && !scenarioToNameMap.containsKey(key)) {
			soKey = event.getTestCase().getUri() + " # " + rowToScenOutMap.get(key).getLocation().getLine();
		}

		scenarioToNameMap.put(soKey, event.getTestCase().getName());
		String scDesc = rowToScenariosMap.get(soKey).getDescription();
		scenarioToDescMap.put(soKey, scDesc == null ? "" : scDesc);
	}

}
