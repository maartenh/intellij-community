/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.idea.svn17.branchConfig;

import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.Nullable;

public class InfoStorage<T> {
  public T myT;
  public InfoReliability myInfoReliability;

  public InfoStorage(final T t, final InfoReliability infoReliability) {
    myT = t;
    myInfoReliability = infoReliability;
  }

  public void accept(final InfoStorage<T> infoStorage, @Nullable final PairConsumer<T, T> callbackOnUpdate) {
    if (infoStorage.myInfoReliability.shouldOverride(myInfoReliability)) {
      if (callbackOnUpdate != null) {
        callbackOnUpdate.consume(myT, infoStorage.myT);
      }
      myT = infoStorage.myT;
      myInfoReliability = infoStorage.myInfoReliability;
    }
  }

  public T getValue() {
    return myT;
  }

  public InfoReliability getInfoReliability() {
    return myInfoReliability;
  }
}
