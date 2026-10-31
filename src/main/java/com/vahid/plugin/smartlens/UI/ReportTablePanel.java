/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vahid.plugin.smartlens.UI;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Common layout of the SmartLens tabs: toolbar, one-line summary and a sortable table.
 * Double-clicking a row opens it via {@link #open(Object)}.
 *
 * @param <T> row type
 */
public abstract class ReportTablePanel<T> extends SimpleToolWindowPanel implements ReportView {
    static final String TOOLBAR_GROUP_ID = "com.vahid.plugin.smartlens.group.Toolbar";

    protected final Project project;
    private final ListTableModel<T> model;
    private final TableView<T> table;
    private final JBLabel summaryLabel = new JBLabel();

    protected ReportTablePanel(Project project, ColumnInfo<T, ?>[] columns, String emptyText) {
        super(true, true);
        this.project = project;
        // No default sort column: rows keep the ranking computed by the analysis until the user sorts
        this.model = new ListTableModel<>(columns, new ArrayList<>());
        this.model.setSortable(true);
        this.table = new TableView<>(model);
        if (table.getRowSorter() != null) {
            table.getRowSorter().setSortKeys(null);
        }
        this.table.getEmptyText().setText(emptyText);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                T selected = table.getSelectedObject();
                if (selected == null) {
                    return false;
                }
                open(selected);
                return true;
            }
        }.installOn(table);

        summaryLabel.setBorder(JBUI.Borders.empty(4, 8));
        JPanel content = new JPanel(new BorderLayout());
        content.add(summaryLabel, BorderLayout.NORTH);
        content.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER);
        setContent(content);

        setToolbar(createToolbar(this));
    }

    /**
     * @return the SmartLens toolbar (Refresh, Reset Attribution) bound to the given tab
     */
    static JComponent createToolbar(JComponent target) {
        ActionGroup toolbarGroup = (ActionGroup) ActionManager.getInstance().getAction(TOOLBAR_GROUP_ID);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("SmartLensToolbar", toolbarGroup, true);
        toolbar.setTargetComponent(target);
        return toolbar.getComponent();
    }

    @Override
    public void showReport(SmartLensReportDto report) {
        model.setItems(new ArrayList<>(rows(report)));
        summaryLabel.setText(summary(report));
    }

    protected abstract List<T> rows(SmartLensReportDto report);

    protected abstract String summary(SmartLensReportDto report);

    protected abstract void open(T item);

    /**
     * @return "Name.ext · relative/directory", so that the file name stays visible when the column is narrow
     */
    protected static String displayPath(Project project, String path) {
        String shown = path;
        String basePath = project.getBasePath();
        if (basePath != null) {
            String relative = FileUtil.getRelativePath(basePath, path, '/');
            if (relative != null && !relative.startsWith("..")) {
                shown = relative;
            }
        }
        int lastSlash = shown.lastIndexOf('/');
        return lastSlash < 0 ? shown : shown.substring(lastSlash + 1) + " · " + shown.substring(0, lastSlash);
    }

    protected void openFile(String path) {
        openFile(project, path);
    }

    static void openFile(Project project, String path) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true);
        }
    }
}
