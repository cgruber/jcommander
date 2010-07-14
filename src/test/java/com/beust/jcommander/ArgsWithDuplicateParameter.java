package com.beust.jcommander;

/**
 * Test multi-object parsing, along with ArgsSlave.
 * 
 * @author cbeust
 */
public class ArgsWithDuplicateParameter {
  @Parameter(names = "-slave")
  public String slave;

  @Parameter(names = "-slave")
  public String anotherSlave;
}
