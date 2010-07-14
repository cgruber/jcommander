package com.beust.jcommander;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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
   * The objects that contain fields annotated with @Parameter.
   */
  private List<Object> m_objects;

  /**
   * This field will contain whatever command line parameter is not an option.
   * It is expected to be a List<String>.
   */
  private Field m_mainParameterField;

  /**
   * The object on which we found the main parameter field.
   */
  private Object m_mainParameterObject;

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
    init(object, null);
  }

  public JCommander(Object object, ResourceBundle bundle, String... args) {
    init(object, bundle);
    parse(args);
  }

  public JCommander(Object object, String... args) {
    init(object, null);
    parse(args);
  }

  private void init(Object object, ResourceBundle bundle) {
    m_bundle = bundle;
    m_objects = Lists.newArrayList();
    if (object instanceof Iterable) {
      // Iterable
      for (Object o : (Iterable) object) {
        m_objects.add(o);
      }
    } else if (object.getClass().isArray()) {
      // Array
      for (Object o : (Object[]) object) {
        m_objects.add(o);
      }
    } else {
      // Single object
      m_objects.add(object);
    }
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

    for (Object object : m_objects) {
      Class<?> cls = object.getClass();
      for (Field f : cls.getDeclaredFields()) {
        p("Field:" + f.getName());
        f.setAccessible(true);
        Annotation annotation = f.getAnnotation(Parameter.class);
        if (annotation != null) {
          Parameter p = (Parameter) annotation;
          if (p.names().length == 0) {
            p("Found main parameter:" + f);
            m_mainParameterField = f;
            m_mainParameterObject = object;
          } else {
            for (String name : p.names()) {
              p("Adding description for " + name);
              ParameterDescription pd = new ParameterDescription(object, p, f, m_bundle);
              m_fields.put(f, pd);
              m_descriptions.put(name, pd);
              if (p.required()) m_requiredFields.put(f, pd);
            }
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
          } else {
            int arity = pd.getParameter().arity();
            int n = (arity != -1 ? arity : 1);  
            if (i + n < args.length) {
              for (int j = 1; j <= n; j++) {
                pd.addValue(trim(args[i + j]));
                m_requiredFields.remove(pd.getField());
              }
              i += n;
            } else {
              throw new ParameterException("Parameter expected after " + args[i]);
            }
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

  /**
   * @return the field that's meant to receive all the parameters that are not options.
   */
  private List<String> getMainParameter() {
    if (m_mainParameterField == null) {
      throw new ParameterException(
          "Non option parameters were found but no main parameter was defined");
    }

    try {
      List<String> result = (List<String>) m_mainParameterField.get(m_mainParameterObject);
      if (result == null) {
        result = Lists.newArrayList();
        m_mainParameterField.set(m_mainParameterObject, result);
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

