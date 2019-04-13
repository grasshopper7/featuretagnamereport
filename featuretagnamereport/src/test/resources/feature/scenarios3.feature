@Features
Feature: Scenario Outline Only

  Background: 
    Background
    Description

    Given this is "THI BACK" step

  @ScenarioOutlines
  Scenario Outline: Scenario Outline 3
    Scenario Outline Three Desc

    When this is "<num>" step

    Examples: 
      | num    |
      | FIRST  |
      | SECOND |

