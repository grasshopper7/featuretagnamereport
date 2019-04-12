@Features
Feature: Scenario and Scenario Outline Combination
  Feature One Description

  @Scenarios
  Scenario: Scenario 1
    Feature One
    Scenario One Description

    Given this is "FIRST" step
    Then this is "SECOND" step

  @ScenarioOutlines
  Scenario Outline: Scenario Outline 1
    Feature One
    Scenario Outline One Description

    Given this is "<num>" step
    When this is "<num>" step

    @Examples
    Examples: 
      | num    |
      | FIRST  |
      | SECOND |
 