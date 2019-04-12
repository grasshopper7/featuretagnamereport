package cucumber.runtime.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestSourceRead;
import gherkin.ast.Background;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;

public class DetailedDryRunFormatter implements EventListener {

	private TestSourcesModel tsm = new TestSourcesModel();

	private Map<String, Feature> uriToFeatureMap = new TreeMap<>();
	private Map<String, Optional<Background>> uriToBackgroundMap = new TreeMap<>();

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
		uriToFeatureMap.put(event.uri, feature);

		List<ScenarioDefinition> sd = feature.getChildren();

		feature.getChildren().stream().forEach(s -> s.getSteps().stream().forEach(st -> {
			System.out.println(st.getText() + "----" + st.getKeyword());
		}));

		sd.stream().forEach(sc -> rowToScenariosMap.put(event.uri + " # " + sc.getLocation().getLine(), sc));

		List<ScenarioOutline> scenout = sd.stream().filter(s -> s instanceof ScenarioOutline)
				.map(so -> (ScenarioOutline) so).collect(Collectors.toList());

		scenout.stream().forEach(so -> so.getExamples().stream().forEach(e -> e.getTableBody().stream()
				.forEach(t -> rowToScenOutMap.put(event.uri + " # " + t.getLocation().getLine(), so))));
	}

	private void handleRunFinished(TestRunFinished event) {

		System.out.println("---Feature Name-------------");
		uriToFeatureMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v.getName());
		});
		System.out.println("---Feature Desc-------------");
		uriToFeatureMap.forEach((k, v) -> {
			System.out.println(k + " -> " + ((v.getDescription() == null) ? "" : v.getDescription()));
		});
		System.out.println("---Background Name----------");
		uriToBackgroundMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v.map(Background::getName).orElse(""));
		});
		System.out.println("---Background Desc----------");
		uriToBackgroundMap.forEach((k, v) -> {
			System.out.println(k + " -> " + v.map(Background::getDescription).orElse(""));
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

	private Optional<Background> optBackground(TestCaseStarted event) {
		TestSourcesModel.AstNode astNode = tsm.getAstNode(event.getTestCase().getUri(), event.getTestCase().getLine());
		return Optional.ofNullable(TestSourcesModel.getBackgroundForTestCase(astNode));
	}

	private void handleCaseStarted(TestCaseStarted event) {

		uriToBackgroundMap.put(event.getTestCase().getUri(), optBackground(event));

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

	class Details {
		String name;
		String description;
	}

	class FeatureDetails extends Details {
		Optional<BackgroundDetails> background;
		List<ScenarioDetails> scenarios;
	}

	class BackgroundDetails extends Details {

	}
	
	class ScenarioDetails extends Details {
		
	}

}
