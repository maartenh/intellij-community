/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.lang.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyIcons;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.highlighter.DefaultHighlighter;
import org.jetbrains.plugins.groovy.lang.completion.handlers.NamedArgumentInsertHandler;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.refactoring.GroovyNamesUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.Priority;

/**
 * @author peter
 */
class MapArgumentCompletionProvider extends CompletionProvider<CompletionParameters> {

  private MapArgumentCompletionProvider() {
  }

  public static void register(CompletionContributor contributor) {
    MapArgumentCompletionProvider instance = new MapArgumentCompletionProvider();

    ElementPattern<PsiElement> inArgumentListOfCall = psiElement().withParent(psiElement(GrReferenceExpression.class).withParent(
      StandardPatterns.or(psiElement(GrArgumentList.class), psiElement(GrListOrMap.class)))
    );

    ElementPattern<PsiElement> inLabel = psiElement(GroovyTokenTypes.mIDENT).withParent(GrArgumentLabel.class);

    contributor.extend(CompletionType.BASIC, inArgumentListOfCall, instance);
    contributor.extend(CompletionType.BASIC, inLabel, instance);
  }

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    PsiElement mapOrArgumentList;

    PsiElement parent = parameters.getPosition().getParent();
    if (parent instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)parent).getQualifier() != null) return;
      mapOrArgumentList = parent.getParent();
    }
    else {
      mapOrArgumentList = parent.getParent().getParent();
    }

    if (mapOrArgumentList instanceof GrListOrMap) {
      if (((GrListOrMap)mapOrArgumentList).getNamedArguments().length > 0) {
        result.stopHere();
      }
    }

    Map<String, NamedArgumentDescriptor> map = calcNamedArgumentsForCall(mapOrArgumentList);
    if (map.isEmpty()) {
      map = findOtherNamedArgumentsInFile(mapOrArgumentList);
    }
    
    for (GrNamedArgument argument : getSiblingNamedArguments(mapOrArgumentList)) {
      map.remove(argument.getLabelName());
    }

    for (Map.Entry<String, NamedArgumentDescriptor> entry : map.entrySet()) {
      LookupElementBuilder lookup = LookupElementBuilder.create(entry.getValue(), entry.getKey())
        .setInsertHandler(NamedArgumentInsertHandler.INSTANCE)
        .setTailText(":");

      if (entry.getValue().getPriority() == Priority.UNLIKELY) {
        lookup.setItemTextForeground(DefaultHighlighter.MAP_KEY_COLOR);
      }
      else {
        lookup = lookup.setIcon(GroovyIcons.DYNAMIC);
      }
      
      result.addElement(lookup);
    }

  }

  private static Map<String, NamedArgumentDescriptor> findOtherNamedArgumentsInFile(PsiElement mapOrArgumentList) {
    final Map<String, NamedArgumentDescriptor> map = new HashMap<String, NamedArgumentDescriptor>();
    mapOrArgumentList.getContainingFile().accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof GrArgumentLabel) {
          final String name = ((GrArgumentLabel)element).getName();
          if (GroovyNamesUtil.isIdentifier(name)) {
            map.put(name, NamedArgumentDescriptor.SIMPLE_UNLIKELY);
          }
        }
        super.visitElement(element);
      }
    });
    return map;
  }

  private static GrNamedArgument[] getSiblingNamedArguments(PsiElement mapOrArgumentList) {
    if (mapOrArgumentList instanceof GrListOrMap) {
      return ((GrListOrMap)mapOrArgumentList).getNamedArguments();
    }

    PsiElement argumentList = mapOrArgumentList instanceof GrArgumentList ? mapOrArgumentList : mapOrArgumentList.getParent();
    if (argumentList instanceof GrArgumentList) {
      if (argumentList.getParent() instanceof GrCall) {
        return PsiUtil.getFirstMapNamedArguments((GrCall)argumentList.getParent());
      }
    }

    return GrNamedArgument.EMPTY_ARRAY;
  }

  private static Map<String, NamedArgumentDescriptor> calcNamedArgumentsForCall(PsiElement mapOrArgumentList) {
    PsiElement argumentList = mapOrArgumentList instanceof GrArgumentList ? mapOrArgumentList : mapOrArgumentList.getParent();
    if (argumentList instanceof GrArgumentList) {
      if (mapOrArgumentList instanceof GrListOrMap) {
        if (((GrArgumentList)argumentList).getNamedArguments().length > 0 ||
            ((GrArgumentList)argumentList).getExpressionArgumentIndex((GrListOrMap)mapOrArgumentList) > 0) {
          return Collections.emptyMap();
        }
      }

      if (argumentList.getParent() instanceof GrCall) {
        return GroovyNamedArgumentProvider.getNamedArgumentsFromAllProviders((GrCall)argumentList.getParent(), null, true);
      }
    }
    return Collections.emptyMap();
  }
}
