Feature: Scenario Outline Only

  @ScenThree
  Scenario Outline: Scenario Outline 3
    Given this is "<num>" step
    When this is "<num>" step

    Examples: 
      | num    |
      | FIRST  |
      | SECOND |
