package com.beust.jcommander;

/**
 * A builder-pattern oriented JCommander
 * 
 * @author christianedwardgruber@gmail.com (Christian Edward Gruber)
 *
 */
public interface ParameterProcessor {
  
  /**
   * Load these objects as parameter value holders and return self
   * for use in 
   * @param objects the objects which will carry the variables
   * @return this.
   */
  public ParameterProcessor in(Object ... objects);
  
  /**
   * Parse the provided arguments into the value holder objects.
   * @param args String arguments (similar to main(String[]))
   */
  public void parse(String ... args); 
  
}
