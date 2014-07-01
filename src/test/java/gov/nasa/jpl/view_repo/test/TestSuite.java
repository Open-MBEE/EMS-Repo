//package gov.nasa.jpl.view_repo.test;
//
//import gov.nasa.jpl.view_repo.util.NodeUtil;
//
//import org.alfresco.service.ServiceRegistry;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.runner.RunWith;
//import org.junit.runners.Suite;
//import org.junit.runners.Suite.SuiteClasses;
//
//@RunWith(Suite.class)
//@SuiteClasses({ EmsSystemModelTest.class })//, JavaQueryTest.class, DemoComponentTest.class, TestLoadClass.class })
//public class TestSuite {
//
//    @BeforeClass
//    public static void setUpClass() {
//        System.out.println("Master setup");
//        ServiceRegistry services = NodeUtil.getServiceRegistry();
//        EmsSystemModelTest.setServiceRegistry( services );
//        JavaQueryTest.setServiceRegistry( services );
//    }
//
//    @AfterClass public static void tearDownClass() { 
//        System.out.println("Master tearDown");
//    }
//
//}