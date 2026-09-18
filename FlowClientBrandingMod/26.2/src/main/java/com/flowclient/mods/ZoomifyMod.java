package com.flowclient.mods;

public final class ZoomifyMod {
  private static boolean enabled;

  private ZoomifyMod() {}

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean value) {
    setEnabled(value, true);
  }

  static void setEnabled(boolean value, boolean persist) {
    enabled = value;
    if (persist) {
      FlowModConfig.save();
    }
  }

  public static void toggle() {
    setEnabled(!enabled);
  }
}
