package com.jusoft.locking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.mockito.Mockito.*;

@Configuration
@EnableAspectJAutoProxy
public class TestConfig {

   public TestConfig() {
   }

   @Bean
   public LockingInterceptor lockingInterceptor() {
      return spy(new LockingInterceptor());
   }


   @Bean
   public LockingInterceptorTest.LockingInterceptorTargetClass lockingInterceptorTargetClass() {
      return new LockingInterceptorTest.LockingInterceptorTargetClass();
   }
}
