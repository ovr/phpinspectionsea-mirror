package com.kalessil.phpStorm.phpInspectionsEA.inspectors.semanticalAnalysis;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpModifierList;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import org.jetbrains.annotations.NotNull;

public class ClassOverridesFieldOfSuperClassInspector extends BasePhpInspection {
    private static final String strProblemDescription      = "Field %p% is already defined in %c%.";
    private static final String strProblemParentOnePrivate = "Likely needs to be protected.";

    @NotNull
    public String getShortName() {
        return "ClassOverridesFieldOfSuperClassInspection";
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpClass(PhpClass clazz) {
                String strClassFQN = clazz.getFQN();
                /** skip un-explorable and test classes */
                if (
                    StringUtil.isEmpty(strClassFQN) ||
                    strClassFQN.contains("\\Tests\\") || strClassFQN.contains("\\Test\\") ||
                    strClassFQN.endsWith("Test")
                ) {
                    return;
                }

                for (PhpClass objParentClass : clazz.getSupers()) {
                    /** ensure class and super are explorable */
                    String strSuperFQN = objParentClass.getFQN();
                    if (objParentClass.isInterface() || null == clazz.getNameIdentifier() || StringUtil.isEmpty(strSuperFQN)) {
                        continue;
                    }

                    for (Field ownField : clazz.getOwnFields()) {
                        /** skip static and un-processable */
                        if (ownField.isConstant() || null == ownField.getNameIdentifier()) {
                            continue;
                        }

                        /** due to lack of api get raw text with all modifiers */
                        String strModifiers = null;
                        for (PsiElement objChild : ownField.getParent().getChildren()) {
                            if (objChild instanceof PhpModifierList) {
                                strModifiers = objChild.getText();
                                break;
                            }
                        }
                        /** skip static variables - they shall not be changed via constructor */
                        if (!StringUtil.isEmpty(strModifiers) && strModifiers.contains("static")) {
                            continue;
                        }


                        String strOwnField = ownField.getName();
                        for (Field superclassField : objParentClass.getOwnFields()) {
                            /** not possible to check access level */
                            if (
                                superclassField.getName().equals(strOwnField) &&
                                ExpressionSemanticUtil.getBlockScope(ownField.getNameIdentifier()) instanceof PhpClass
                            /** php doc can re-define property type */
                            ) {
                                /** find modifiers list and check if super declares private field */
                                String strSuperModifiers = null;
                                for (PsiElement objChild : superclassField.getParent().getChildren()) {
                                    if (objChild instanceof PhpModifierList) {
                                        strSuperModifiers = objChild.getText();
                                        break;
                                    }
                                }
                                final boolean isPrivate = (!StringUtil.isEmpty(strSuperModifiers) && strSuperModifiers.contains("private"));

                                /** prepare message, make it helpful */
                                String strWarning = strProblemDescription
                                        .replace("%p%", strOwnField)
                                        .replace("%c%", strSuperFQN);
                                if (isPrivate) {
                                    strWarning += strProblemParentOnePrivate;
                                }

                                /** fire warning */
                                holder.registerProblem(ownField.getParent(), strWarning, ProblemHighlightType.WEAK_WARNING);
                                break;
                            }
                        }
                    }
                }
            }
        };
    }
}