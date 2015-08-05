package com.jusoft.locking;

public class AnnotatedArgumentNotFound extends RuntimeException {
   private static final String MESSAGE = "Annotated argument for locking template not found";

   public AnnotatedArgumentNotFound() {
      super(MESSAGE);
   }
}
