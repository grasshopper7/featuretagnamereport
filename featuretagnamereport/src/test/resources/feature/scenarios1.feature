@Feature
Feature: Scenario and Scenario Outline Combination
  Hell hell hell

  @ScenarioOne
  Scenario: Scenario 1
    hello hello

    And this is "FIRST" step
    And this is "SECOND" step

  @ScenarioOutlineOne
  Scenario Outline: Scenario Outline 1
  why why
  not not 
  at all
    Given this is "<num>" step
    When this is "<num>" step

    @SOExamplesOne
    Examples: 
      | num    |
      | FIRST  |
      | SECOND |
