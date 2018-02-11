package org.kincajou;

import com.intellij.lang.LanguageImportStatements;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class Component implements ApplicationComponent {

  private static final String COMPONENT_NAME = "Java Imports Cleaner";

  @Override
  public void initComponent() {
    LanguageImportStatements.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaImportCleaner());
  }

  @NotNull
  @Override
  public String getComponentName() {
    return COMPONENT_NAME;
  }
}
