package com.beust.jcommander;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

public class JCommanderTest {
  @Test
  public void simpleArgs() {
    Args1 args = new Args1();
    String[] argv = { "-debug", "-log", "2", "-groups", "unit", "a", "b", "c" };
    new JCommander(args, argv);

    Assert.assertTrue(args.debug);
    Assert.assertEquals(args.verbose.intValue(), 2);
    Assert.assertEquals(args.groups, "unit");
    Assert.assertEquals(args.parameters, Arrays.asList("a", "b", "c"));
  }

  /**
   * Make sure that if there are args with multiple names (e.g. "-log" and "-verbose"),
   * the usage will only display it once.
   */
  @Test
  public void repeatedArgs() {
    Args1 args = new Args1();
    String[] argv = { "-log", "2" };
    JCommander jc = new JCommander(args, argv);
    Assert.assertEquals(jc.getParameters().size(), 3);
  }

  /**
   * Not specifying a required option should throw an exception.
   */
  @Test(expectedExceptions = ParameterException.class)
  public void requiredFields1() {
    Args1 args = new Args1();
    String[] argv = { "-debug" };
    new JCommander(args, argv);
  }

  /**
   * Required options with multiple names should work with all names.
   */
  @Test
  public void requiredFields2() {
    Args1 args = new Args1();
    String[] argv = { "-log", "2" };
    new JCommander(args, argv);
  }

  /**
   * Required options with multiple names should work with all names.
   */
  @Test
  public void requiredFields3() {
    Args1 args = new Args1();
    String[] argv = { "-verbose", "2" };
    new JCommander(args, argv);
  }

  private void i18n(Locale locale, String expectedString) {
    ResourceBundle bundle = locale != null ? ResourceBundle.getBundle("MessageBundle", locale)
        : null;

    I18N i18n = new I18N();
    String[] argv = { "-host", "localhost" };
    JCommander jc = new JCommander(i18n, bundle, argv);
//    jc.usage();

    ParameterDescription pd = jc.getParameters().get(0);
    Assert.assertEquals(pd.getDescription(), expectedString);
  }

  @Test
  public void i18nNoLocale() {
    i18n(null, "Host");
  }

  @Test
  public void i18nUsLocale() {
    i18n(new Locale("en", "US"), "Host");
  }

  @Test
  public void i18nFrLocale() {
    i18n(new Locale("fr", "FR"), "H�te");
  }

  @Test
  public void multiObjects() {
    ArgsMaster m = new ArgsMaster();
    ArgsSlave s = new ArgsSlave();
    String[] argv = { "-master", "master", "-slave", "slave" };
    new JCommander(new Object[] { m , s }, argv);

    Assert.assertEquals(m.master, "master");
    Assert.assertEquals(s.slave, "slave");
  }

  @Test(expectedExceptions = ParameterException.class)
  public void multiObjectsWithDuplicates() {
    ArgsMaster m = new ArgsMaster();
    ArgsSlave s = new ArgsSlaveBogus();
    String[] argv = { "-master", "master", "-slave", "slave" };
    new JCommander(new Object[] { m , s }, argv);
  }

  @Test
  public void arityString() {
    ArgsArityString args = new ArgsArityString();
    String[] argv = { "-pairs", "pair0", "pair1", "rest" };
    new JCommander(args, argv);

    Assert.assertEquals(args.pairs.size(), 2);
    Assert.assertEquals(args.pairs.get(0), "pair0");
    Assert.assertEquals(args.pairs.get(1), "pair1");
    Assert.assertEquals(args.rest.size(), 1);
    Assert.assertEquals(args.rest.get(0), "rest");
  }

  @Test(expectedExceptions = ParameterException.class)
  public void arityFail1() {
    ArgsArityString args = new ArgsArityString();
    String[] argv = { "-pairs", "pair0" };
    new JCommander(args, argv);
  }

  @Test(expectedExceptions = ParameterException.class)
  public void multipleUnparsedFail() {
    ArgsMultipleUnparsed args = new ArgsMultipleUnparsed();
    String[] argv = { };
    new JCommander(args, argv);
  }
  
  @Test(expectedExceptions = ParameterException.class)
  public void singleParameterWithDuplicates() {
    ArgsWithDuplicateParameter args = new ArgsWithDuplicateParameter();
    String[] argv = { "-slave" };
    new JCommander(args, argv);
  }
  public static void main(String[] args) {
//    new JCommanderTest().multiObjects();
    new JCommanderTest().multipleUnparsedFail();
  }
  
}
