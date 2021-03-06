/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.libraries;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class LibraryDetectionManager {
  public static LibraryDetectionManager getInstance() {
    return ServiceManager.getService(LibraryDetectionManager.class);
  }

  public abstract boolean processProperties(@NotNull List<VirtualFile> files, @NotNull LibraryPropertiesProcessor processor);

  @Nullable
  public abstract Pair<LibraryType<?>, LibraryProperties<?>> detectType(@NotNull List<VirtualFile> files);

  public interface LibraryPropertiesProcessor {
    <P extends LibraryProperties> boolean processProperties(@NotNull LibraryKind<P> kind, @NotNull P properties);
  }
}
