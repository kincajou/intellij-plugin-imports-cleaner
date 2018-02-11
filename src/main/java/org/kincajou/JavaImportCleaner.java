package org.kincajou;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public class JavaImportCleaner extends JavaImportOptimizer {

  private static final Logger LOG = Logger.getInstance("#com.intellij.lang.java.JavaImportOptimizer");

  @NotNull
  @Override
  public Runnable processFile(PsiFile file) {
    if (!(file instanceof PsiJavaFile)) {
      return EmptyRunnable.getInstance();
    }
    Project project = file.getProject();
    final PsiImportList newImportList = JavaCodeStyleManager.getInstance(project).prepareOptimizeImportsResult((PsiJavaFile) file);
    if (newImportList == null) {
      return EmptyRunnable.getInstance();
    }

    return new CollectingInfoRunnable() {
      private int myImportsAdded;
      private int myImportsRemoved;

      @Override
      public void run() {
        try {
          final PsiDocumentManager manager = PsiDocumentManager.getInstance(file.getProject());
          final Document document = manager.getDocument(file);
          if (document != null) {
            manager.commitDocument(document);
          }
          final PsiImportList oldImportList = ((PsiJavaFile) file).getImportList();
          assert oldImportList != null;

          for (PsiImportStatementBase statement : oldImportList.getAllImportStatements()) {
            deleteExistingStatement(statement, newImportList.getAllImportStatements(), oldImportList);
          }

          final Multiset<PsiElement> oldImports = HashMultiset.create();
          for (PsiImportStatement statement : oldImportList.getImportStatements()) {
            oldImports.add(statement.resolve());
          }

          final Multiset<PsiElement> oldStaticImports = HashMultiset.create();
          for (PsiImportStaticStatement statement : oldImportList.getImportStaticStatements()) {
            oldStaticImports.add(statement.resolve());
          }

          for (PsiImportStatement statement : newImportList.getImportStatements()) {
            if (!oldImports.remove(statement.resolve())) {
              myImportsAdded++;
            }
          }
          myImportsRemoved += oldImports.size();

          for (PsiImportStaticStatement statement : newImportList.getImportStaticStatements()) {
            if (!oldStaticImports.remove(statement.resolve())) {
              myImportsAdded++;
            }
          }
          myImportsRemoved += oldStaticImports.size();
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }

      private void deleteExistingStatement(PsiImportStatementBase statement, PsiImportStatementBase[] newStatements, PsiImportList oldList) {
        boolean exists = Arrays.stream(newStatements).map(PsiImportStatementBase::getText).anyMatch(t -> statement.getText().equals(t));
        if (!exists) {
          oldList.deleteChildRange(statement, statement);
        }
      }

      @Override
      public String getUserNotificationInfo() {
        if (myImportsRemoved == 0) {
          return "rearranged imports";
        }
        final StringBuilder notification = new StringBuilder("removed ").append(myImportsRemoved).append(" import");
        if (myImportsRemoved > 1) {
          notification.append('s');
        }
        if (myImportsAdded > 0) {
          notification.append(", added ").append(myImportsAdded).append(" import");
          if (myImportsAdded > 1) {
            notification.append('s');
          }
        }
        return notification.toString();
      }
    };
  }
}
