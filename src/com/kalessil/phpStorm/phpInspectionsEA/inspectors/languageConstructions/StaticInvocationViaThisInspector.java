package com.kalessil.phpStorm.phpInspectionsEA.inspectors.languageConstructions;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.jetbrains.annotations.NotNull;

public class StaticInvocationViaThisInspector extends BasePhpInspection {
    private static final String strProblemDescription = "'static::...' shall be used instead";

    @NotNull
    public String getShortName() {
        return "StaticInvocationViaThisInspection";
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            /** Static fields will be simply not resolved properly, so we can not do checking for them */

            public void visitPhpMethodReference(MethodReference reference) {
                PsiReference objReference = reference.getReference();
                if (null != objReference) {
                    PsiElement objResolvedRef = objReference.resolve();
                    /** resolved method is static but called with $ this*/
                    if (
                        objResolvedRef instanceof Method &&
                        ((Method) objResolvedRef).isStatic() &&
                        reference.getFirstChild().getText().equals("$this")
                    ) {
                        /** report an issue */
                        holder.registerProblem(reference.getFirstChild(), strProblemDescription, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }
            }
        };
    }
}