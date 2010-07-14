package com.beust.jcommander;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Arrays;
import java.util.List;

public class JMultiCommanderTest {
 
  private static final String[] ARGV = { "-log", "2", "-groups", "unit", "a", "b", "c" };

  
  public static class TestingValueHolder1 {
    @Parameter
    public List<String> parameters = Lists.newArrayList();
  
    @Parameter(names = { "-log", "-verbose" }, description = "Level of verbosity")
    public Integer verbose = 1;
  }
  
  public static class TestingValueHolder2 {
    @Parameter(names = "-groups", description = "Comma-separated list of group names to be run")
    public String groups;
  
    @Parameter(names = "-debug", description = "Debug mode")
    public boolean debug = false;
  }
  
  public static class TestingDuplicateMainParamterHolder {
    @Parameter
    public List<String> parms = Lists.newArrayList();
  }

  public static class TestingDuplicateNamedParamterHolder {
    @Parameter(names = "-debug", description = "DebugMode")
    public Boolean debug = false;
  }
  
  @Test
  public void simpleArgs() {
    JMultiCommander jct = new JMultiCommander();
    TestingValueHolder1 tvh1 = new TestingValueHolder1();
    TestingValueHolder2 tvh2 = new TestingValueHolder2();
    jct.in(tvh1, tvh2).parse(ARGV);

    System.out.println("Verbose:" + tvh1.verbose);
    Assert.assertEquals(tvh1.verbose.intValue(), 2);
    Assert.assertEquals(tvh2.groups, "unit");
    Assert.assertEquals(tvh1.parameters, Arrays.asList("a", "b", "c"));
  }
  
  @Test(expectedExceptions={ParameterException.class})
  public void duplicateConflictingArgs() {
    JMultiCommander jct = new JMultiCommander();
    TestingValueHolder2 parm = new TestingValueHolder2();
    TestingDuplicateNamedParamterHolder dup = new TestingDuplicateNamedParamterHolder();
    jct.in(parm, dup).parse(ARGV);
  }
  
  @Test(expectedExceptions={ParameterException.class})
  public void duplicateConflictingDefaultArgs() {
    JMultiCommander jct = new JMultiCommander();
    TestingValueHolder1 parm = new TestingValueHolder1();
    TestingDuplicateMainParamterHolder dup = new TestingDuplicateMainParamterHolder();
    jct.in(parm, dup).parse(ARGV);
  }
}
