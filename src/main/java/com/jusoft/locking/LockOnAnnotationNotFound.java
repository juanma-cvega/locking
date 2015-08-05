package com.jusoft.locking;

public class LockOnAnnotationNotFound extends RuntimeException {

   private final static String MESSAGE = "Annotation LockOn not found";

   public LockOnAnnotationNotFound() {
      super(MESSAGE);
   }
}
