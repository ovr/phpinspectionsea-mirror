package com.kalessil.phpStorm.phpInspectionsEA.inspectors.semanticalAnalysis;


import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.jetbrains.annotations.NotNull;

public class SingletonFactoryPatternViolationInspector extends BasePhpInspection {
    private static final String strProblemDescription = "Ensure appropriate method is defined and public: getInstance, create, create*";

    @NotNull
    public String getDisplayName() {
        return "Architecture: class violates singleton/factory pattern definition";
    }

    @NotNull
    public String getShortName() {
        return "SingletonFactoryPatternViolationInspection";
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpClass(PhpClass clazz) {
                Method objConstructor = clazz.getOwnConstructor();
                if (
                    null == objConstructor ||
                    !objConstructor.getAccess().isProtected() ||
                    null == clazz.getNameIdentifier()
                ) {
                    return;
                }

                Method getInstance = clazz.findOwnMethodByName("getInstance");
                boolean hasGetInstance = (null != getInstance && getInstance.getAccess().isPublic());
                if (hasGetInstance) {
                    return;
                }

                for (Method ownMethod: clazz.getOwnMethods()) {
                    if (ownMethod.getName().startsWith("create")) {
                        return;
                    }
                }

                holder.registerProblem(clazz.getNameIdentifier(), strProblemDescription, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
        };
    }
}