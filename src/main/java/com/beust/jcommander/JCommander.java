package com.beust.jcommander;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The main class for JCommander. It's responsible for parsing the object that contains
 * all the annotated fields, parse the command line and assign the fields with the correct
 * values and a few other helper methods, such as usage().
 * 
 * @author cbeust
 */
public class JCommander {
  /**
   * A map to look up parameter description per option name.
   */
  private Map<String, ParameterDescription> m_descriptions;

  /**
   * The object that contains the fields annotated with @Parameter.
   */
  private Object m_object;

  /**
   * This field will contain whatever command line parameter is not an option.
   * It is expected to be a List<String>.
   */
  private Field m_mainParameterField;

  /**
   * A set of all the fields that are required. During the reflection phase,
   * this field receives all the fields that are annotated with required=true
   * and during the parsing phase, all the fields that are assigned a value
   * are removed from it. At the end of the parsing phase, if it's not empty,
   * then some required fields did not receive a value and an exception is
   * thrown.
   */
  private Map<Field, ParameterDescription> m_requiredFields = Maps.newHashMap();

  /**
   * A map of all the annotated fields.
   */
  private Map<Field, ParameterDescription> m_fields = Maps.newHashMap();

  private ResourceBundle m_bundle;

  public JCommander(Object object) {
    m_object = object;
  }

  public JCommander(Object object, ResourceBundle bundle, String... args) {
    m_object = object;
    m_bundle = bundle;
    parse(args);
  }

  public JCommander(Object object, String... args) {
    m_object = object;
    parse(args);
  }

  /**
   * Parse the command line parameters.
   */
  public void parse(String... args) {
    createDescriptions();
    parseValues(expandArgs(args));
    validateOptions();
  }

  /**
   * Make sure that all the required parameters have received a value.
   */
  private void validateOptions() {
    if (! m_requiredFields.isEmpty()) {
      StringBuilder missingFields = new StringBuilder();
      for (ParameterDescription pd : m_requiredFields.values()) {
        missingFields.append(pd.getNames()[0]).append(" ");
      }
      throw new ParameterException("The following options are required: " + missingFields);
    }
    
  }
  
  /**
   * Expand the command line parameters to take @ parameters into account.
   * When @ is encountered, the content of the file that follows is inserted
   * in the command line.
   * 
   * @param originalArgv the original command line parameters
   * @return the new and enriched command line parameters
   */
  private static String[] expandArgs(String[] originalArgv) {
    List<String> vResult = Lists.newArrayList();
    
    for (String arg : originalArgv) {

      if (arg.startsWith("@")) {
        String fileName = arg.substring(1);
        vResult.addAll(readFile(fileName));
      }
      else {
        vResult.add(arg);
      }
    }
    
    return vResult.toArray(new String[vResult.size()]);
  }

  /**
   * Reads the file specified by filename and returns the file content as a string.
   * End of lines are replaced by a space
   * 
   * @param fileName the command line filename
   * @return the file content as a string.
   */
  public static List<String> readFile(String fileName) {
    List<String> result = Lists.newArrayList();

    try {
      BufferedReader bufRead = new BufferedReader(new FileReader(fileName));

      String line;

      // Read through file one line at time. Print line # and line
      while ((line = bufRead.readLine()) != null) {
        result.add(line);
      }

      bufRead.close();
    }
    catch (IOException e) {
      throw new ParameterException("Could not read file " + fileName + ": " + e);
    }

    return result;
  }

  /**
   * @param string
   * @return
   */
  private static String trim(String string) {
    String result = string.trim();
    if (result.startsWith("\"")) {
      if (result.endsWith("\"")) {
          return result.substring(1, result.length() - 1);
      } else {
          return result.substring(1);
      }
    } else {
      return result;
    }
  }

  private void createDescriptions() {
    m_descriptions = Maps.newHashMap();
    Class<?> cls = m_object.getClass();
    for (Field f : cls.getDeclaredFields()) {
      p("Field:" + f.getName());
      f.setAccessible(true);
      Annotation annotation = f.getAnnotation(Parameter.class);
      if (annotation != null) {
        Parameter p = (Parameter) annotation;
        if (p.names().length == 0) {
          p("Found main parameter:" + f);
          m_mainParameterField = f;
        } else {
          for (String name : p.names()) {
            p("Adding description for " + name);
            ParameterDescription pd = new ParameterDescription(m_object, p, f, m_bundle);
            m_fields.put(f, pd);
            m_descriptions.put(name, pd);
            if (p.required()) m_requiredFields.put(f, pd);
          }
        }
      }
    }
  }

  private void p(String string) {
    if (false) {
      System.out.println("[JCommander] " + string);
    }
  }

  private void parseValues(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String a = trim(args[i]);
      if (a.startsWith("-")) {
        ParameterDescription pd = m_descriptions.get(a);
        if (pd != null) {
          Class<?> fieldType = pd.getField().getType();
          if (fieldType == boolean.class || fieldType == Boolean.class) {
            pd.addValue(Boolean.TRUE);
            m_requiredFields.remove(pd.getField());
          } else if (i + 1 < args.length) {
            pd.addValue(trim(args[i + 1]));
            m_requiredFields.remove(pd.getField());
            i++;
          } else {
            throw new ParameterException("Parameter expected after " + args[i]);
          }
        } else {
          throw new ParameterException("Unknown option: " + a);
        }
      }
      else {
        if (! isStringEmpty(args[i])) getMainParameter().add(args[i]);
      }
    }
  }

  private static boolean isStringEmpty(String s) {
    return s == null || "".equals(s);
  }

  private List<String> getMainParameter() {
    if (m_mainParameterField == null) {
      throw new ParameterException(
          "Non option parameters were found but no main parameter was defined");
    }

    try {
      List<String> result = (List<String>) m_mainParameterField.get(m_object);
      if (result == null) {
        result = Lists.newArrayList();
        m_mainParameterField.set(m_object, result);
      }
      return result;
    }
    catch(IllegalAccessException ex) {
      throw new ParameterException("Couldn't access main parameter: " + ex.getMessage());
    }
  }

  /**
   * Display a the help on System.out.
   */
  public void usage() {
    System.out.println("Usage:");
    for (ParameterDescription pd : m_fields.values()) {
      StringBuilder sb = new StringBuilder();
      for (String n : pd.getParameter().names()) {
        sb.append(n).append(" ");
      }
      System.out.println("\t" + sb.toString() + "\t" + pd.getDescription());
    }
  }

  /**
   * @return a Collection of all the @Parameter annotations found on the
   * target class. This can be used to display the usage() in a different
   * format (e.g. HTML).
   */
  public List<ParameterDescription> getParameters() {
    return new ArrayList(m_fields.values());
  }
}

