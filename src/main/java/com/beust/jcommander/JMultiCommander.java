package com.beust.jcommander;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JMultiCommander implements ParameterProcessor {
  private Map<String, ParameterDescription> m_descriptions;
  private List<Object> m_objects;

  /** This field will contain whatever command line parameter is not an option.
   * It is expected to be a List<String>.
   */
  private Field m_mainParameterField;
  private Object m_mainParameterObject;

  public JMultiCommander() { }

  @Override
  public ParameterProcessor in(Object ... objects) {
    m_objects = Arrays.asList(objects);
    return this;
  }

  @Override
  public void parse(String ... args) {
    createDescriptions();
    parseValues(expandArgs(args));
  }

  /**
   * Expand the command line parameters to take @ parameters into account.
   * When @ is encountered, the content of the file that follows is inserted
   * in the command line
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
    for (Object obj : m_objects) {
      Class<?> cls = obj.getClass();
      for (Field f : cls.getDeclaredFields()) {
        p("Field:" + f.getName());
        f.setAccessible(true);
        Annotation annotation = f.getAnnotation(Parameter.class);
        if (annotation != null) {
          Parameter p = (Parameter) annotation;
          if (p.names().length == 0) {
            p("Found main parameter:" + f);
            if (m_mainParameterField != null) {
              throw new ParameterException(String.format(
                  "Both %s and %s declare an un-named @Parameter", m_mainParameterObject, obj));
            }
            m_mainParameterField = f;
            m_mainParameterObject = obj;
          } else {
            for (String name : p.names()) {
              p("Adding description for " + name);
              ParameterDescription pd = new ParameterDescription(obj, p, f);
              if (m_descriptions.containsKey(name)) {
                throw new ParameterException("More than one value holder declares a paramter named: " + name);
              }
              m_descriptions.put(name, pd);
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

  private String[] parseValues(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String a = trim(args[i]);
      if (a.startsWith("-")) {
        ParameterDescription pd = m_descriptions.get(a);
        if (pd != null) {
          Class<?> fieldType = pd.getField().getType();
          if (fieldType == boolean.class || fieldType == Boolean.class) {
            pd.addValue(Boolean.TRUE);
          } else if (i + 1 < args.length) {
            pd.addValue(trim(args[i + 1]));
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
    return new String[] {};
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

  public void usage() {
    System.out.println("Usage:");
    for (ParameterDescription pd : m_descriptions.values()) {
      StringBuilder sb = new StringBuilder();
      for (String n : pd.getNames()) {
        sb.append(n).append(" ");
      }
      System.out.println("\t" + sb.toString() + "\t" + pd.getDescription());
    }
  }
}
