package com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.kalessil.phpStorm.phpInspectionsEA.gui.PrettyListControl;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import net.miginfocom.swing.MigLayout;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class ForgottenDebugOutputInspector extends BasePhpInspection {
    // custom configuration, automatically saved between restarts so keep out of changing modifiers
    public LinkedList<String> configuration = new LinkedList<String>();
    private HashSet<String> customFunctions = new HashSet<String>();
    private HashMap<String, Pair<String, String>> customMethods = new HashMap<String, Pair<String, String>>();
    private HashSet<String> customMethodsNames = new HashSet<String>();

    // prepared content for smooth runtime
    static private final String strProblemDescription = "Please ensure this is not a forgotten debug statement";

    public ForgottenDebugOutputInspector() {
    }

    public void readSettings(@NotNull Element node) throws InvalidDataException {
        XmlSerializer.deserializeInto(this, node);
        recompileConfiguration();
    }

    private void recompileConfiguration() {
        customFunctions.clear();
        customMethods.clear();
        customMethodsNames.clear();

        if (0 == configuration.size()) {
            return;
        }

        // parse what was provided
        for (String stringDescriptor : configuration) {
            stringDescriptor = stringDescriptor.trim();
            if (!stringDescriptor.contains("::")) {
                customFunctions.add(stringDescriptor);
                continue;
            }

            String[] disassembledDescriptor = stringDescriptor.split("::", 2);
            customMethods.put(
                    stringDescriptor.toLowerCase(),
                    Pair.create(disassembledDescriptor[0], disassembledDescriptor[1])
            );
            customMethodsNames.add(disassembledDescriptor[1]);
        }
    }

    @NotNull
    public String getShortName() {
        return "ForgottenDebugOutputInspection";
    }

    private HashMap<String, Integer> functionsRequirements = null;
    private HashMap<String, Integer> getFunctionsRequirements() {
        if (null == functionsRequirements) {
            functionsRequirements = new HashMap<String, Integer>();

            /* function name => amount of arguments considered legal */
            functionsRequirements.put("print_r",               2);
            functionsRequirements.put("var_export",            2);
            functionsRequirements.put("var_dump",              -1);
            functionsRequirements.put("debug_zval_dump",       -1);
            functionsRequirements.put("debug_print_backtrace", -1);
        }

        return functionsRequirements;
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpMethodReference(MethodReference reference) {
                final String name = reference.getName();
                if (0 == customMethods.size() || StringUtil.isEmpty(name) || !customMethodsNames.contains(name)) {
                    return;
                }

                for (Pair<String, String> match : customMethods.values()) {
                    if (!name.equals(match.getSecond())) {
                        continue;
                    }

                    // resolve as method
                    PsiElement resolved = reference.resolve();
                    if (!(resolved instanceof Method)) {
                        continue;
                    }

                    // analyze if class as needed
                    PhpClass clazz = ((Method) resolved).getContainingClass();
                    if (null != clazz) {
                        String classFqn = clazz.getFQN();
                        if (!StringUtil.isEmpty(classFqn) && match.getFirst().equals(classFqn)) {
                            holder.registerProblem(reference, strProblemDescription, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                            return;
                        }
                    }
                }
            }

            public void visitPhpFunctionCall(FunctionReference reference) {
                final String strFunction              = reference.getName();
                HashMap<String, Integer> requirements = getFunctionsRequirements();
                if (
                    !StringUtil.isEmpty(strFunction) && requirements.containsKey(strFunction) &&
                    reference.getParameters().length != requirements.get(strFunction)
                ) {
                    holder.registerProblem(reference, strProblemDescription, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    return;
                }

                // user-defined functions
                if (customFunctions.contains(strFunction)) {
                    holder.registerProblem(reference, strProblemDescription, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }
        };
    }

    public JComponent createOptionsPanel() {
        return (new ForgottenDebugOutputInspector.OptionsPanel()).getComponent();
    }

    public class OptionsPanel {
        private JPanel optionsPanel;

        public OptionsPanel() {
            optionsPanel = new JPanel();
            optionsPanel.setLayout(new MigLayout());

            // inject controls
            optionsPanel.add(new JLabel("Custom debug methods:"), "wrap");
            optionsPanel.add((new PrettyListControl(configuration) {
                protected void fireContentsChanged() {
                    recompileConfiguration();
                    super.fireContentsChanged();
                }
            }).getComponent(), "pushx, growx");
        }

        public JPanel getComponent() {
            return optionsPanel;
        }
    }
}
