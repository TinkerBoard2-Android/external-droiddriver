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

package com.google.android.droiddriver.instrumentation;

import static com.google.android.droiddriver.util.TextUtils.charSequenceToString;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.TextView;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.base.AbstractUiElement;
import com.google.common.base.Preconditions;

/**
 * A UiElement that is backed by a View.
 */
// TODO: always accessing view on the UI thread even when only get access is
// needed -- the field may be in the middle of updating.
public class ViewElement extends AbstractUiElement {
  private final InstrumentationContext context;
  private final View view;
  private String className;

  public ViewElement(InstrumentationContext context, View view) {
    this.context = Preconditions.checkNotNull(context);
    this.view = Preconditions.checkNotNull(view);
  }

  @Override
  public String getText() {
    if (!(view instanceof TextView)) {
      return null;
    }
    return charSequenceToString(((TextView) view).getText());
  }

  @Override
  public String getContentDescription() {
    return charSequenceToString(view.getContentDescription());
  }

  @Override
  public String getClassName() {
    if (className != null) {
      return className;
    }
    // createAccessibilityNodeInfo is expensive and, surprisingly, it calls
    // setText, which requires it be called on the UI thread.
    context.getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        AccessibilityNodeInfo accessibilityNodeInfo = view.createAccessibilityNodeInfo();
        className = charSequenceToString(accessibilityNodeInfo.getClassName());
        accessibilityNodeInfo.recycle();
      }
    });
    return className;
  }

  @Override
  public String getResourceId() {
    if (view.getId() != View.NO_ID) {
      try {
        return charSequenceToString(view.getResources().getResourceName(view.getId()));
      } catch (Resources.NotFoundException nfe) {
        /* ignore */
      }
    }
    return null;
  }

  @Override
  public String getPackageName() {
    return view.getContext().getPackageName();
  }

  @Override
  public InputInjector getInjector() {
    return context.getInjector();
  }

  @Override
  public boolean isVisible() {
    // TODO: use getVisibleBounds() once it's in
    // isShown() checks the visibility flag of this view and ancestors; it needs
    // to have the VISIBLE flag as well as non-empty bounds to be visible.
    return view.isShown() && !getBounds().isEmpty();
  }

  @Override
  public boolean isCheckable() {
    return view instanceof Checkable;
  }

  @Override
  public boolean isChecked() {
    if (!isCheckable()) {
      return false;
    }
    return ((Checkable) view).isChecked();
  }

  @Override
  public boolean isClickable() {
    return view.isClickable();
  }

  @Override
  public boolean isEnabled() {
    return view.isEnabled();
  }

  @Override
  public boolean isFocusable() {
    return view.isFocusable();
  }

  @Override
  public boolean isFocused() {
    return view.isFocused();
  }

  @Override
  public boolean isScrollable() {
    // TODO: find a meaningful implementation
    return true;
  }

  @Override
  public boolean isLongClickable() {
    return view.isLongClickable();
  }

  @Override
  public boolean isPassword() {
    // TODO: find a meaningful implementation
    return false;
  }

  @Override
  public boolean isSelected() {
    return view.isSelected();
  }

  @Override
  public Rect getBounds() {
    Rect rect = new Rect();
    int[] xy = new int[2];
    view.getLocationOnScreen(xy);
    rect.set(xy[0], xy[1], xy[0] + view.getWidth(), xy[1] + view.getHeight());
    return rect;
  }

  @Override
  public int getChildCount() {
    if (!(view instanceof ViewGroup)) {
      return 0;
    }
    return ((ViewGroup) view).getChildCount();
  }

  @Override
  public ViewElement getChild(int index) {
    if (!(view instanceof ViewGroup)) {
      return null;
    }
    View child = ((ViewGroup) view).getChildAt(index);
    return child == null ? null : context.getUiElement(child);
  }

  @Override
  public ViewElement getParent() {
    ViewParent parent = view.getParent();
    if (!(parent instanceof View)) {
      return null;
    }
    return context.getUiElement((View) parent);
  }

  public View getView() {
    return view;
  }
}
