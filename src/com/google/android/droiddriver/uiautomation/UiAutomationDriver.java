/*
 * Copyright (C) 2013 DroidDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.droiddriver.uiautomation;

import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.droiddriver.Screenshotter;
import com.google.android.droiddriver.base.AbstractDroidDriver;
import com.google.android.droiddriver.exceptions.TimeoutException;
import com.google.android.droiddriver.util.FileUtils;
import com.google.android.droiddriver.util.Logs;
import com.google.common.primitives.Longs;

import java.io.BufferedOutputStream;

/**
 * Implementation of a DroidDriver that is driven via the accessibility layer.
 */
public class UiAutomationDriver extends AbstractDroidDriver implements Screenshotter {
  // TODO: magic const from UiAutomator, but may not be useful
  /**
   * This value has the greatest bearing on the appearance of test execution
   * speeds. This value is used as the minimum time to wait before considering
   * the UI idle after each action.
   */
  private static final long QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE = 500;// ms

  private final UiAutomationContext context;
  private final UiAutomation uiAutomation;

  public UiAutomationDriver(UiAutomation uiAutomation) {
    this.uiAutomation = uiAutomation;
    this.context = new UiAutomationContext(uiAutomation);
  }

  @Override
  public UiAutomationElement getRootElement() {
    return context.getUiElement(getRootNode());
  }

  private AccessibilityNodeInfo getRootNode() {
    long timeoutMillis = getPoller().getTimeoutMillis();
    try {
      uiAutomation.waitForIdle(QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE, timeoutMillis);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new TimeoutException(e);
    }
    long end = SystemClock.uptimeMillis() + timeoutMillis;
    while (true) {
      AccessibilityNodeInfo root = uiAutomation.getRootInActiveWindow();
      if (root != null) {
        return root;
      }
      long remainingMillis = end - SystemClock.uptimeMillis();
      if (remainingMillis < 0) {
        throw new TimeoutException(
            String.format("Timed out after %d milliseconds waiting for root AccessibilityNodeInfo",
                timeoutMillis));
      }
      SystemClock.sleep(Longs.min(250, remainingMillis));
    }
  }

  @Override
  public boolean takeScreenshot(String path) {
    return takeScreenshot(path, Bitmap.CompressFormat.PNG, 0);
  }

  @Override
  public boolean takeScreenshot(String path, CompressFormat format, int quality) {
    Logs.call(this, "takeScreenshot", path, quality);
    Bitmap screenshot = uiAutomation.takeScreenshot();
    if (screenshot == null) {
      return false;
    }
    BufferedOutputStream bos = null;
    try {
      bos = FileUtils.open(path);
      screenshot.compress(format, quality, bos);
      return true;
    } catch (Exception e) {
      Logs.log(Log.WARN, e);
      return false;
    } finally {
      if (bos != null) {
        try {
          bos.close();
        } catch (Exception e) {
          // ignore
        }
      }
      screenshot.recycle();
    }
  }
}
