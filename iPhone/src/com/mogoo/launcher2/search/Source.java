/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mogoo.launcher2.search;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

/**
 * Interface for suggestion sources.
 *
 */
public interface Source {

    /**
     * Gets the name of the activity that this source is for. When a suggestion is
     * clicked, the resulting intent will be sent to this activity.
     */
    ComponentName getComponentName();

    /**
     * Gets the localized, human-readable label for this source.
     */
    CharSequence getLabel();

    /**
     * Gets the icon URI for this suggestion source.
     */
    Uri getSourceIconUri();

    /**
     * Gets an icon from this suggestion source.
     *
     * @param drawableId Resource ID or URI.
     */
    Drawable getIcon();

    /**
     * Gets the search hint text for this suggestion source.
     */
    CharSequence getHint();

    Intent getIntent();
    
    CharSequence getOther();
    

}
