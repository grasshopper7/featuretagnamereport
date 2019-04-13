Feature: 

  Background: Background Name
    When this is "FOU BACK" step

  @ScenOne @ScenTwo
  Scenario: 
    Then these are steps
      | num | word  |
      |   1 | one   |
      |   2 | two   |
      |   3 | three |

  @ScenTwo @ScenThree
  Scenario: 
    Then these are steps
      | num | word |
      |   1 | one  |
      |   2 | two  |

  Scenario: 
    And this is "FIRST" step

  Scenario Outline: Scenario Outline 3
    Scenario Outline Three Desc

    When this is "<num>" step

    Examples: 
      | num    |
      | FIRST  |
      | SECOND |
