/*
 * The MIT License
 *
 * Copyright 2017 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.ui;

import com.intuit.karate.ScriptValueMap;
import com.intuit.karate.cucumber.CucumberUtils;
import com.intuit.karate.cucumber.StepResult;
import com.intuit.karate.cucumber.StepWrapper;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 *
 * @author pthomas3
 */
public class StepPanel extends AnchorPane {

    private static final Logger logger = LoggerFactory.getLogger(StepPanel.class);

    private final AppSession session;
    private final TextArea textArea;
    private final Button runButton;
    private Optional<Button> runAllUptoButton = Optional.empty();
    private final Optional<StepPanel> previousPanel;
    private final Optional<TextArea> rawRequestResponse;
    private String oldText;
    private StepWrapper step;
    private Boolean pass = null;

    private static final String STYLE_PASS = "-fx-base: #53B700";
    private static final String STYLE_FAIL = "-fx-base: #D52B1E";
    private static final String STYLE_METHOD = "-fx-base: #34BFFF";
    private static final String STYLE_DEFAULT = "-fx-base: #F0F0F0";
    private static final String STYLE_BACKGROUND = "-fx-text-fill: #8D9096";
    public static final String STYLE_HTTP_METHOD = "-fx-border-color: #D1D1D1";

    public StepPanel(AppSession session, StepWrapper step, Optional<StepPanel> previousPanel) {
        this.session = session;
        this.previousPanel = previousPanel;
        runButton = new Button("►");
        textArea = new TextArea();
        textArea.setFont(App.getDefaultFont());
        textArea.setMinHeight(0);
        textArea.setWrapText(true);
        textArea.focusedProperty().addListener((val, before, after) -> {
            if (!after) { // if we lost focus
                rebuildFeatureIfTextChanged();
            }
        });
        this.step = step;
        initTextArea();
        runButton.setOnAction(e -> run());
        if(step.isHttpCall()) {
            BorderPane borderPane = new BorderPane();
            borderPane.setPadding(new Insets(5, 0, 0, 0));
            borderPane.setStyle(STYLE_HTTP_METHOD);
            AnchorPane anchorPane = new AnchorPane();
            setUpTextAndRunButtons(previousPanel, anchorPane.getChildren(), anchorPane);
            rawRequestResponse =  Optional.of(new TextArea());
            TitledPane titledPane = new TitledPane("View raw Request/Response", rawRequestResponse.get());
            Accordion accordion = new Accordion();
            accordion.setPadding(new Insets(5, 5, 5, 5));
            accordion.setStyle(STYLE_DEFAULT);
            accordion.getPanes().addAll(titledPane);

            borderPane.setTop(anchorPane);
            borderPane.setBottom(accordion);
            getChildren().add(borderPane);
            setLeftAnchor(borderPane, 0.0);
            setRightAnchor(borderPane, 0.0);
            setTopAnchor(borderPane, 0.0);
            setBottomAnchor(borderPane, 0.0);
        } else {
            rawRequestResponse = Optional.empty();
            setUpTextAndRunButtons(previousPanel, getChildren(), this);
        }
    }

    private void setUpTextAndRunButtons(Optional<StepPanel> previousPanel, ObservableList<Node> children, AnchorPane anchorPane) {
        children.addAll(textArea, runButton);
        setUpRunAllUptoButton(previousPanel, children, anchorPane);
        anchorPane.setLeftAnchor(textArea, 0.0);
        anchorPane.setRightAnchor(textArea, 72.0);
        anchorPane.setBottomAnchor(textArea, 0.0);
        anchorPane.setRightAnchor(runButton, 0.0);
        anchorPane.setTopAnchor(runButton, 2.0);
        anchorPane.setBottomAnchor(runButton, 0.0);
    }

    private void setUpRunAllUptoButton(Optional<StepPanel> previousPanel, ObservableList<Node> children, AnchorPane anchorPane) {
        if(previousPanel.isPresent()) {
            final Button button = new Button("►►");
            runAllUptoButton = Optional.of(button);
            button.setTooltip(new Tooltip("Run all steps upto current step"));
            button.setOnAction(e -> runAllUpto());
            children.add(button);
            anchorPane.setRightAnchor(button, 32.0);
            anchorPane.setTopAnchor(button, 2.0);
            anchorPane.setBottomAnchor(button, 0.0);
        }
    }
    
    private void rebuildFeatureIfTextChanged() {
        String newText = textArea.getText();
        if (!newText.equals(oldText)) {
            session.replace(step, newText);
        }        
    }
    
    private void run() {
        rebuildFeatureIfTextChanged();
        StepResult result = CucumberUtils.runCalledStep(step, session.backend);
        pass = result.isPass();
        initStyleColor();
        session.refreshVarsTable();
        rawRequestResponse.ifPresent( r -> updateRawRequestResponse(r));
        if (!pass) {
            throw new StepException(result);
        }
    }

    private void updateRawRequestResponse(TextArea textArea) {
        StringBuilder text = new StringBuilder();
        text.append("Request "+System.lineSeparator());
        session.getVars().stream().filter(v -> v.getName().contains(ScriptValueMap.VAR_REQUEST))
                .forEach(v -> text.append(getLine(v)));
        text.append(System.lineSeparator());
        text.append("Response "+System.lineSeparator());
        session.getVars().stream().filter(v -> v.getName().contains(ScriptValueMap.VAR_RESPONSE))
                .forEach(v -> text.append(getLine(v)));
        textArea.setText(text.toString());
    }

    private String getLine(Var var) {
        return var.getName() + " : " + (var.getValue() != null ? var.getValue().getAsPrettyString() : "") + System.lineSeparator();
    }

    private void runAllUpto() {
        previousPanel.ifPresent(p -> p.runAllUpto());
        run();
    }

    private void setStyleForRunAllUptoButton(String style) {
        runAllUptoButton.ifPresent(b -> b.setStyle(style));
    }

    public void action(AppAction action) {
        switch (action) {
            case REFRESH:
                step = session.refresh(step);
                initTextArea();
                break;
            case RESET:
                pass = null;
                initStyleColor();
                break;
            case RUN:
                if (pass == null) {
                    run();
                }
                break;
        }
    }
    
    private void initStyleColor() {
        if (pass == null) {
            runButton.setStyle("");
            setStyleForRunAllUptoButton("");
        } else if (pass) {
            runButton.setStyle(STYLE_PASS);
            setStyleForRunAllUptoButton(STYLE_PASS);
        } else {
            runButton.setStyle(STYLE_FAIL);
            setStyleForRunAllUptoButton(STYLE_FAIL);
        }       
    }

    private void initTextArea() {
        oldText = step.getText();
        textArea.setText(oldText);
        int lineCount = step.getLineCount();
        if (lineCount == 1) {
            int wrapEstimate = oldText.length() / 40;
            if (wrapEstimate > 1) {
                lineCount = wrapEstimate;
            } else {
                lineCount = 0;
            }
        }
        textArea.setPrefRowCount(lineCount);
        if (step.isHttpCall()) {
            setStyle(STYLE_METHOD);
            textArea.setStyle(STYLE_METHOD);
        } else {
            setStyle(STYLE_DEFAULT);
        }
        if (step.isBackground()) {
            textArea.setStyle(STYLE_BACKGROUND);
        }
        initStyleColor();
    }

}
