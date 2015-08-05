package com.jusoft.locking;

import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext
public class LockingInterceptorTest {

   private static final TestClass TEST_OBJECT_1 = new TestClass(new TestClass.InnerClass("testField1"));
   private static final TestClass TEST_OBJECT_1_DIFFERENT_MONITOR = new TestClass(new TestClass.InnerClass("testField2"));

   private static final Integer TEST_INTEGER = 100;
   private static final Integer TEST_DIFFERENT_INTEGER = 200;
   public static final String NEW_VALUE_FROM_THREAD_1 = "THREAD1";
   public static final String NEW_VALUE_FROM_THREAD_2 = "THREAD2";

   @Mock
   private TestClass testClass;

   @Autowired
   private LockingInterceptor lockingInterceptor;

   @Autowired
   private LockingInterceptorTargetClass targetClass;

   @Captor
   private ArgumentCaptor<ProceedingJoinPoint> pjpCaptor;

   @Before
   public void setup() {
      initMocks(this);
   }

   @After
   public void tearDown() {
      reset(lockingInterceptor);
   }

   @Test
   public void pointcutMatchesWithOneParameter() throws Throwable {
      targetClass.annotationWithOneParameter(1);

      verify(lockingInterceptor).lock(pjpCaptor.capture());
   }

   @Test
   public void pointcutMatchesWithThreeParametersAndFirstWithAnnotation() throws Throwable {
      targetClass.annotationWithThreeParametersAndFirstWithAnnotation(1, 2L, "test");

      verify(lockingInterceptor).lock(pjpCaptor.capture());
   }

   @Test
   public void pointcutMatchesWithThreeParametersAndSecondWithAnnotation() throws Throwable {
      targetClass.annotationWithThreeParametersAndSecondWithAnnotation(1, 2L, "test");

      verify(lockingInterceptor).lock(pjpCaptor.capture());
   }

   @Test
   public void pointcutMatchesWithThreeParametersAndThirdWithAnnotation() throws Throwable {
      targetClass.annotationWithThreeParametersAndThirdWithAnnotation(1, 2L, "test");

      verify(lockingInterceptor).lock(pjpCaptor.capture());
   }

   @Test
   public void adviseLocksOnSameMonitorFromComplexObject() throws ExecutionException, InterruptedException {
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      Future<Object> thread1 = executorService.submit(() -> {
         targetClass.annotationOnComplexObject(TEST_OBJECT_1, 1000, NEW_VALUE_FROM_THREAD_1);
         return null;
      });
      sleep(100); //Ensures ordering of threads in acquiring the lock
      Future<Object> thread2 = executorService.submit(() -> {
         targetClass.annotationOnComplexObject(TEST_OBJECT_1, 500, NEW_VALUE_FROM_THREAD_2);
         return null;
      });

      thread1.get();
      thread2.get();

      assertThat(targetClass.getField()).isEqualTo(NEW_VALUE_FROM_THREAD_2);
   }

   @Test
   public void adviseLocksOnDifferentMonitorFromComplexObject() throws ExecutionException, InterruptedException {
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      Future<Object> thread1 = executorService.submit(() -> {
         targetClass.annotationOnComplexObject(TEST_OBJECT_1, 1000, NEW_VALUE_FROM_THREAD_1);
         return null;
      });
      Future<Object> thread2 = executorService.submit(() -> {
         targetClass.annotationOnComplexObject(TEST_OBJECT_1_DIFFERENT_MONITOR, 500, NEW_VALUE_FROM_THREAD_2);
         return null;
      });

      thread1.get();
      thread2.get();

      assertThat(targetClass.getField()).isEqualTo(NEW_VALUE_FROM_THREAD_1);
   }

   @Test
   public void adviseLocksOnSameMonitorWithNoFieldSpecified() throws ExecutionException, InterruptedException {
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      Future<Object> thread1 = executorService.submit(() -> {
         targetClass.annotationOnSimpleObject(TEST_INTEGER, 1000, NEW_VALUE_FROM_THREAD_1);
         return null;
      });
      Future<Object> thread2 = executorService.submit(() -> {
         targetClass.annotationOnSimpleObject(TEST_INTEGER, 500, NEW_VALUE_FROM_THREAD_2);
         return null;
      });

      thread1.get();
      thread2.get();

      assertThat(targetClass.getField()).isEqualTo(NEW_VALUE_FROM_THREAD_2);
   }

   @Test
   public void adviseLocksOnDifferentMonitorWithNoFieldSpecified() throws ExecutionException, InterruptedException {
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      Future<Object> thread1 = executorService.submit(() -> {
         targetClass.annotationOnSimpleObject(TEST_INTEGER, 1000, NEW_VALUE_FROM_THREAD_1);
         return null;
      });
      Future<Object> thread2 = executorService.submit(() -> {
         targetClass.annotationOnSimpleObject(TEST_DIFFERENT_INTEGER, 500, NEW_VALUE_FROM_THREAD_2);
         return null;
      });

      thread1.get();
      thread2.get();

      assertThat(targetClass.getField()).isEqualTo(NEW_VALUE_FROM_THREAD_1);
   }

   public static class LockingInterceptorTargetClass {

      private String field;

      public void annotationWithOneParameter(@LockOn Integer parameter) {
      }

      public void annotationWithThreeParametersAndFirstWithAnnotation(@LockOn Integer parameter1, Long parameter2, String parameter3) {

      }

      public void annotationWithThreeParametersAndSecondWithAnnotation(Integer parameter1, @LockOn Long parameter2, String parameter3) {

      }

      public void annotationWithThreeParametersAndThirdWithAnnotation(Integer parameter1, Long parameter2, @LockOn String parameter3) {

      }

      public void annotationOnComplexObject(@LockOn("innerClass.innerClassField") LockingInterceptorTest.TestClass testObject, int sleepTime, String newValue) throws InterruptedException {
         sleep(sleepTime);
         field = newValue;
      }

      public void annotationOnSimpleObject(@LockOn Integer testMonitor, int sleepTime, String newValue) throws InterruptedException {
         sleep(sleepTime);
         field = newValue;
      }

      public String getField() {
         return field;
      }
   }

   @Data
   public static class TestClass {

      private String field;
      private final InnerClass innerClass;

      @Data
      public static class InnerClass {
         private final String innerClassField;
      }
   }
}
