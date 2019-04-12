@Feature
Feature: Scenario and Scenario Outline Combination
  Feature One Description

  @ScenarioOne
  Scenario: Scenario 1
    Feature One
    Scenario One Description

    And this is "FIRST" step
    And this is "SECOND" step

  @ScenarioOutlineOne
  Scenario Outline: Scenario Outline 1
    Feature One
    Scenario Outline One Description

    Given this is "<num>" step
    When this is "<num>" step

    @SOExamplesOne
    Examples: 
      | num    |
      | FIRST  |
      | SECOND |

  Scenario Outline: Scenario Outline 2
    Feature Two
    Scenario Outline Two Description

    Given this is "<num>" step
    When this is "<num>" step

    Examples: 
      | num    |
      | FIRST  |
      | SECOND |
