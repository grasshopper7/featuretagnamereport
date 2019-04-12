package cucumber.runtime.formatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import gherkin.ast.Step;

public class AdvancedDryRunFormatter implements EventListener {

	private TestSourcesModel tsm = new TestSourcesModel();

	private Map<String, FeatureDetails> uriToFeatureMap = new TreeMap<>();

	private Map<String, ScenarioOutline> rowToScenOutMap = new TreeMap<>();
	private Map<String, ScenarioDefinition> rowToScenariosMap = new TreeMap<>();

	private Map<String, List<String>> tagToScenariosMap = new TreeMap<>();
	private Map<String, List<String>> scenarioToTagsMap = new TreeMap<>();

	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestSourceRead.class, this::handleSourceRead);
		publisher.registerHandlerFor(TestCaseStarted.class, this::handleCaseStarted);
		publisher.registerHandlerFor(TestRunFinished.class, this::handleRunFinished);
	}

	private String getOptionalDetails(String details) {
		return Optional.ofNullable(details).orElse("");
	}

	private void handleSourceRead(TestSourceRead event) {
		tsm.addTestSourceReadEvent(event.uri, event);

		Feature feature = tsm.getFeature(event.uri);
		uriToFeatureMap.put(event.uri, new FeatureDetails(feature));

		List<ScenarioDefinition> sd = feature.getChildren();

		/*feature.getChildren().stream().forEach(s -> s.getSteps().stream().forEach(st -> {
			System.out.println(st.getText() + "----" + st.getKeyword() + "----" + st.getArgument());
		}));*/

		sd.stream().forEach(sc -> rowToScenariosMap.put(event.uri + " # " + sc.getLocation().getLine(), sc));

		List<ScenarioOutline> scenout = sd.stream().filter(s -> s instanceof ScenarioOutline)
				.map(so -> (ScenarioOutline) so).collect(Collectors.toList());

		scenout.stream().forEach(so -> so.getExamples().stream().forEach(e -> e.getTableBody().stream()
				.forEach(t -> rowToScenOutMap.put(event.uri + " # " + t.getLocation().getLine(), so))));
	}

	private Optional<BackgroundDetails> optBackground(TestCaseStarted event) {
		TestSourcesModel.AstNode astNode = tsm.getAstNode(event.getTestCase().getUri(), event.getTestCase().getLine());

		Optional<Background> bg = Optional.ofNullable(astNode)
				.map(n -> TestSourcesModel.getBackgroundForTestCase(astNode));
		return bg.map(b -> new BackgroundDetails(b));
	}

	private void handleCaseStarted(TestCaseStarted event) {

		FeatureDetails fd = uriToFeatureMap.get(event.getTestCase().getUri());
		fd.background = optBackground(event);

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
		if (rowToScenOutMap.containsKey(key))
			soKey = event.getTestCase().getUri() + " # " + rowToScenOutMap.get(key).getLocation().getLine();

		ScenarioDefinition sc = rowToScenariosMap.get(soKey);
		Set<ScenarioDetails> scDets = new HashSet<>();
		scDets.add(new ScenarioDetails(sc, soKey));
		fd.scenarios.addAll(scDets);
	}

	private void handleRunFinished(TestRunFinished event) {
		
		System.out.println(rowToScenariosMap);

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

		System.out.println("---Feature Details--------");
		uriToFeatureMap.forEach((uri, fd) -> {
			System.out.println("----------------");
			System.out.println("Uri - " + uri);
			System.out.println("Feature Name - " + fd.name);
			System.out.println("Feature Description - " + fd.description);
			Optional<BackgroundDetails> back = fd.background;
			System.out.println("Background Name - " + back.map(b -> b.name).orElse(""));
			System.out.println("Background Description - " + back.map(b -> b.description).orElse(""));
			back.map(b -> b.steps).orElse(new HashSet<>()).stream().forEach(s -> {
				System.out.println("Background Step Text - " + s.text);
				System.out.println("Background Step Keyword - " + s.keyword);
				System.out.println("Background Step Argument - " + s.argument);
			});
			Set<ScenarioDetails> scen = fd.scenarios;
			scen.stream().forEach(sc -> {
				System.out.println("Scenario Name - " + sc.name);
				System.out.println("Scenario Description = " + sc.description);
				sc.steps.stream().forEach(s -> {
					System.out.println("Step Text - " + s.text);
					System.out.println("Step Keyword - " + s.keyword);
					System.out.println("Step Argument - " + s.argument);
				});
			});
		});

	}

	class Details {
		String name;
		String description;

		Details(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}

	class FeatureDetails extends Details {
		Optional<BackgroundDetails> background;
		Set<ScenarioDetails> scenarios = new HashSet<>();

		FeatureDetails(String name, String description) {
			super(name, description);
		}

		FeatureDetails(Feature feature) {
			super(feature.getName(), getOptionalDetails(feature.getDescription()));
		}
	}

	class BackgroundDetails extends Details {
		Set<StepDetails> steps = new HashSet<>();		

		BackgroundDetails(String name, String description) {
			super(name, description);
		}

		BackgroundDetails(Background background) {
			super(background.getName(), background.getDescription());
			background.getSteps().stream().forEach(s -> {
				steps.add(new StepDetails(s));
			});
		}
	}

	class ScenarioDetails extends Details {
		String uri;
		Set<StepDetails> steps = new HashSet<>();

		ScenarioDetails(String name, String description, String uri) {
			super(name, description);
			this.uri = uri;
		}
		
		ScenarioDetails(ScenarioDefinition sd, String uri) {
			this(sd.getName(), sd.getDescription(), uri);
			this.uri = uri;
			sd.getSteps().stream().forEach(s -> {
				steps.add(new StepDetails(s));
			});
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(uri);
		}

		@Override
		public boolean equals(Object obj) {
			return Objects.equals(this.uri, ((ScenarioDetails) obj).uri);
		}
	}

	class StepDetails {
		String text;
		String keyword;
		Object argument;

		StepDetails(String text, String keyword, Object argument) {
			this.text = text;
			this.keyword = keyword;
			this.argument = argument;
		}

		StepDetails(Step step) {
			this.text = step.getText();
			this.keyword = step.getKeyword();
			this.argument = step.getArgument();
		}
	}

}
