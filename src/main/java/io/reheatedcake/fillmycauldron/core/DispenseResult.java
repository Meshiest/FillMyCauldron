package io.reheatedcake.fillmycauldron.core;

/**
 * The `DispenseResult` enum represents the possible results of a dispenser
 * behavior.
 */
public enum DispenseResult {
  /** Do nothing */
  NOOP,
  /** Use the fallback behavior */
  FALLBACK,
  /** Continue with the custom behavior */
  CONTINUE,
}
