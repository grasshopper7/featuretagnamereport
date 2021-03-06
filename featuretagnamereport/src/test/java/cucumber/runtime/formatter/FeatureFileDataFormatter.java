package cucumber.runtime.formatter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestSourceRead;
import cucumber.api.formatter.NiceAppendable;
import gherkin.ast.Background;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;

public class FeatureFileDataFormatter implements EventListener {

	private TestSourcesModel tsm = new TestSourcesModel();

	private Map<String, FeatureDetails> uriToFeatureMap = new TreeMap<>();

	private Map<String, ScenarioOutline> rowToScenOutMap = new TreeMap<>();
	private Map<String, ScenarioDefinition> lineToScenariosMap = new TreeMap<>();
	
	private Set<String> scenariosNoTags = new TreeSet<>();

	private Map<String, List<String>> tagToScenariosMap = new TreeMap<>();
	private Map<String, List<String>> scenarioToTagsMap = new TreeMap<>();

	private NiceAppendable out;

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

		sd.stream().filter(s -> !(s instanceof Background)).forEach(sc -> lineToScenariosMap.put(event.uri + " # " + sc.getLocation().getLine(), sc));
		
		scenariosNoTags.addAll(lineToScenariosMap.keySet());
		
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
				
			if (rowToScenOutMap.containsKey(key)) {
				String soKey = event.getTestCase().getUri() + " # " + rowToScenOutMap.get(key).getLocation().getLine(); 
				scen.add(soKey);
				scenariosNoTags.remove(soKey);
			} else {
				scen.add(key);
				scenariosNoTags.remove(key);
			}

			tagToScenariosMap.merge(t.getName(), scen, (l1, l2) -> {
				if (!tagToScenariosMap.get(t.getName()).contains(l2.get(0)))
					l1.addAll(l2);
				return l1;
			});
		});

		String soKey = key;
		if (rowToScenOutMap.containsKey(key))
			soKey = event.getTestCase().getUri() + " # " + rowToScenOutMap.get(key).getLocation().getLine();

		ScenarioDefinition sc = lineToScenariosMap.get(soKey);
		Set<ScenarioDetails> scDets = new LinkedHashSet<>();
		scDets.add(new ScenarioDetails(sc, soKey));
		fd.scenarios.addAll(scDets);
	}

	private void appendNewLine(String line) {
		out.append(line + System.getProperty("line.separator"));
	}

	private void handleRunFinished(TestRunFinished event) {
		
		appendNewLine("---Feature Details--------");
		uriToFeatureMap.forEach((uri, fd) -> {
			appendNewLine("Uri - " + uri);
			appendNewLine("Feature Name - " + fd.name);
			appendNewLine("Feature Description - " + fd.description);
			Optional<BackgroundDetails> back = fd.background;
			back.ifPresent(b -> {
				appendNewLine("Background Name - " + b.name);
				appendNewLine("Background Description - " + b.description);
				appendNewLine("Background Steps -------- ");
			});
			back.ifPresent(b -> {
				b.steps.stream().forEach(s -> {
					appendNewLine("\tBackground Step Text - " + s.text);
					appendNewLine("\tBackground Step Keyword - " + s.keyword);
					appendNewLine("\tBackground Step Argument - " + ((s.argument == null) ? "" : s.argument));
					appendNewLine("\t-------------------- ");
				});
			});
			Set<ScenarioDetails> scen = fd.scenarios;
			appendNewLine("Scenarios --------");
			scen.stream().forEach(sc -> {
				appendNewLine("Scenario Name - " + sc.name);
				appendNewLine("Scenario Description - " + sc.description);
				if (sc.steps.size() > 0)
					appendNewLine("Scenario Steps -------- ");
				sc.steps.stream().forEach(s -> {
					appendNewLine("\tStep Text - " + s.text);
					appendNewLine("\tStep Keyword - " + s.keyword);
					appendNewLine("\tStep Argument - " + ((s.argument == null) ? "" : s.argument));
					appendNewLine("\t-------------------- ");
				});
			});
			appendNewLine("--------------------");
			appendNewLine("");
		});

		appendNewLine("");

		appendNewLine("---Tags to Scenario---------");
		tagToScenariosMap.forEach((k, v) -> {
			appendNewLine(k);
			v.forEach(l -> appendNewLine("\t" + l));
		});

		appendNewLine("");
		appendNewLine("");

		appendNewLine("---Scenarios to Tag---------");
		tagToScenariosMap.keySet().stream().forEach(k -> tagToScenariosMap.get(k).forEach(v -> {
			List<String> key = new ArrayList<>();
			key.add(k);
			scenarioToTagsMap.merge(v, key, (l1, l2) -> {
				l1.addAll(l2);
				return l1;
			});
		}));
		scenarioToTagsMap.forEach((k, v) -> {
			appendNewLine(k);
			v.forEach(l -> appendNewLine("\t" + l));
		});
		
		appendNewLine("");
		appendNewLine("");

		appendNewLine("---Scenarios without Tags---------");
		scenariosNoTags.stream().forEach(s -> {
			appendNewLine(s);
		});
		
		out.println();
		out.close();
	}

	public FeatureFileDataFormatter() {
		out = new NiceAppendable(System.out);
	}

	public FeatureFileDataFormatter(URL repDir) {
		try {
			out = new NiceAppendable(
					new OutputStreamWriter(new URLOutputStream((new URL(repDir, "report.log"))), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		Set<ScenarioDetails> scenarios = new LinkedHashSet<>();

		FeatureDetails(String name, String description) {
			super(name, description);
		}

		FeatureDetails(Feature feature) {
			super(feature.getName(), getOptionalDetails(feature.getDescription()));
		}
	}

	class BackgroundDetails extends Details {
		Set<StepDetails> steps = new LinkedHashSet<>();

		BackgroundDetails(String name, String description) {
			super(name, description == null ? "" : description);
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
		Set<StepDetails> steps = new LinkedHashSet<>();

		ScenarioDetails(String name, String description, String uri) {
			super(name, description == null ? "" : description);
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
