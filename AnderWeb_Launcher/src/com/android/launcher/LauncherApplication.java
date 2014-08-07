/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher;

import android.app.Application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LauncherApplication extends Application {
    private static Object runtime = null;    
    private final static float TARGET_HEAP_UTILIZATION = 0.75f;    
    private final static int CWJ_HEAP_SIZE = 6* 1024* 1024 ;
    
    @Override
    public void onCreate() {
        //VMRuntime.getRuntime().setTargetHeapUtilization(TARGET_HEAP_UTILIZATION);
        //VMRuntime.getRuntime().setMinimumHeapSize(4 * 1024 * 1024);
        try {
            Class<?> cl = Class.forName("dalvik.system.VMRuntime");
            Method getRt = cl.getMethod("getRuntime", new Class[0]);
            runtime = getRt.invoke(null, new Object[0]);
            
            final Class<?>[] longArgsClass = new Class[1];
            longArgsClass[0] = Long.TYPE;
            Method setMiniHeaP = runtime.getClass().getMethod("setMinimumHeapSize", longArgsClass);
            setMiniHeaP.invoke(runtime, CWJ_HEAP_SIZE);
            
            final Class<?>[] floatArgsClass = new Class[1];
            floatArgsClass[0] = Float.TYPE;
            Method setTargetHeapUtilization = runtime.getClass().getMethod("setTargetHeapUtilization", floatArgsClass);
            setTargetHeapUtilization.invoke(runtime, TARGET_HEAP_UTILIZATION);
        } catch (ClassNotFoundException e) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        super.onCreate();
    }
}
