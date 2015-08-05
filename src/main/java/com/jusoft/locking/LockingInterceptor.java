package com.jusoft.locking;

import com.google.common.collect.Interner;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

import static com.google.common.collect.Interners.newWeakInterner;
import static java.util.Arrays.asList;

/**
 * Aspect in charge of finding parameters annotated with {@see com.iggroup.wt.majormarketmovements.locking.LockOn}.
 * The annotated parameter will be used to synchronized the method on the object annotated.
 * If specified in the annotation, a field from a complex object can be used as the monitor.
 * This annotation can only be set in one parameter, it doesn't matter which position it is on.
 *
 * @author carnicj
 */
@Aspect
public class LockingInterceptor {

   private static final String DOT_SEPARATOR = "\\.";
   private static final Interner<Object> INTERNER = newWeakInterner();

   @Pointcut("execution(public * *(..,@LockOn (*),..))")
   public void lockOnAnnotationPoincut() {

   }

   @Around("lockOnAnnotationPoincut()")
   public Object lock(ProceedingJoinPoint pjp) throws Throwable {
      Object[] arguments = pjp.getArgs();
      Annotation[][] annotations = getAnnotationsInParameters(pjp);

      String monitorFieldName = findAnnotationValue(annotations);
      Object annotatedArgument = getAnnotatedArgument(arguments, annotations);

      Object monitor = getMonitor(annotatedArgument, monitorFieldName);
      Object internedMonitor = INTERNER.intern(monitor);

      synchronized (internedMonitor) {
         return pjp.proceed(arguments);
      }
   }

   private Annotation[][] getAnnotationsInParameters(ProceedingJoinPoint pjp) throws NoSuchMethodException {
      MethodSignature signature = (MethodSignature) pjp.getSignature();
      String methodName = signature.getMethod().getName();
      Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
      return pjp.getTarget().getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();
   }

   private String findAnnotationValue(Annotation[][] annotations) {
      for (int i = 0; i < annotations.length; i++) {
         Optional<Annotation> lockOnAnnotation = asList(annotations[i])
               .stream().filter(annotation -> annotation instanceof LockOn).findFirst();
         if (lockOnAnnotation.isPresent()) {
            return ((LockOn) lockOnAnnotation.get()).value();
         }
      }
      throw new LockOnAnnotationNotFound();
   }

   private Object getAnnotatedArgument(Object[] arguments, Annotation[][] annotations) {
      int argumentIndex = findArgumentIndexFromAnnotations(annotations);
      return arguments[argumentIndex];
   }

   private int findArgumentIndexFromAnnotations(Annotation[][] annotations) {
      for (int argumentIndex = 0; argumentIndex < annotations.length; argumentIndex++) {
         for (int annotationIndex = 0; annotationIndex < annotations[argumentIndex].length; annotationIndex++) {
            if (annotations[argumentIndex][annotationIndex] instanceof LockOn) {
               return argumentIndex;
            }
         }
      }
      throw new AnnotatedArgumentNotFound();
   }

   private Object getMonitor(Object monitor, String monitorFieldName) throws NoSuchFieldException, IllegalAccessException {
      if (StringUtils.EMPTY.equals(monitorFieldName)) {
         return monitor;
      }
      String[] monitorFieldParts = monitorFieldName.split(DOT_SEPARATOR);
      for (String fieldPart : monitorFieldParts) {
         Field declaredField = monitor.getClass().getDeclaredField(fieldPart);
         declaredField.setAccessible(true);
         monitor = declaredField.get(monitor);
      }
      return monitor;
   }
}
