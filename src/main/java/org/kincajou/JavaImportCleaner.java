package org.kincajou;

import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import java.util.Arrays;
import java.util.Optional;
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

          myImportsRemoved = (int) Arrays.stream(oldImportList.getAllImportStatements())
            .map(s -> deleteExistingStatement(s, newImportList, oldImportList))
            .filter(d -> d)
            .count();

          // imports can be added when weight of * wildcard is high, so optimizing will remove star import and add actual ones
          PsiImportStatementBase previous = null;
          for (PsiImportStatementBase statement : newImportList.getAllImportStatements()) {
            boolean added = addNewStatement(previous, statement, oldImportList);
            previous = statement;
            if (added) {
              myImportsAdded++;
            }
          }
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }

      private boolean deleteExistingStatement(PsiImportStatementBase statement, PsiImportList newList, PsiImportList oldList) {
        boolean exists = find(statement, newList).isPresent();
        if (!exists) {
          oldList.deleteChildRange(statement, statement);
        }
        return !exists;
      }

      private boolean addNewStatement(PsiImportStatementBase previous, PsiImportStatementBase current, PsiImportList oldList) {
        Optional<PsiImportStatementBase> existing = find(current, oldList);
        if (existing.isPresent()) {
          return false;
        }
        if (previous == null) {
          // previous is null, add at the beginning
          if (oldList.getAllImportStatements().length == 0) {
            // no import statements, just add
            oldList.add(current.copy());
          }
          else {
            // add before first
            oldList.addBefore(current.copy(), oldList.getAllImportStatements()[0]);
          }
          return true;
        }
        Optional<PsiImportStatementBase> previousExisting = find(previous, oldList);
        // previousExisting always exists because it was either just added or was already there from the start
        oldList.addAfter(current.copy(), previousExisting.get());
        return true;
      }

      private Optional<PsiImportStatementBase> find(PsiImportStatementBase statement, PsiImportList list) {
        return Arrays.stream(list.getAllImportStatements()).filter(s -> statement.getText().equals(s.getText())).findFirst();
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
