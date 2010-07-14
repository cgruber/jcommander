package com.beust.jcommander;

import java.util.List;

/**
 * Test parameter arity.
 * 
 * @author cbeust
 */
public class ArgsArityInteger {

  @Parameter(names = "-pairs", arity = 2, description = "Pairs")
  public List<Integer> pairs;

  @Parameter(description = "Rest")
  public List<String> rest;
}
