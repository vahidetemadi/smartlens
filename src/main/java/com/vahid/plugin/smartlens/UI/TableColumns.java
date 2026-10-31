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

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Factory of sortable table columns: values are rendered as text but sorted by their underlying value.
 */
final class TableColumns {
    private TableColumns() {
    }

    static <T> ColumnInfo<T, String> text(String name, Function<T, String> value) {
        return column(name, value, Comparator.comparing(value, String.CASE_INSENSITIVE_ORDER), null);
    }

    static <T> ColumnInfo<T, String> number(String name, ToIntFunction<T> value) {
        return column(name, item -> String.valueOf(value.applyAsInt(item)), Comparator.comparingInt(value), "00000");
    }

    static <T> ColumnInfo<T, String> percent(String name, ToDoubleFunction<T> value) {
        return column(name, item -> Math.round(value.applyAsDouble(item) * 100) + "%", Comparator.comparingDouble(value), "100%");
    }

    static <T> ColumnInfo<T, String> score(String name, ToDoubleFunction<T> value) {
        return column(name, item -> String.format("%.1f", value.applyAsDouble(item)), Comparator.comparingDouble(value), "100.0");
    }

    /**
     * @param preferredValue sample value giving numeric columns a fixed width, null to share the remaining width
     */
    private static <T> ColumnInfo<T, String> column(String name, Function<T, String> value, Comparator<T> comparator,
                                                    @Nullable String preferredValue) {
        return new ColumnInfo<>(name) {
            @Override
            public @Nullable String valueOf(T item) {
                return value.apply(item);
            }

            @Override
            public @NotNull Comparator<T> getComparator() {
                return comparator;
            }

            @Override
            public int getWidth(JTable table) {
                if (preferredValue == null) {
                    return super.getWidth(table);
                }
                FontMetrics metrics = table.getFontMetrics(table.getFont());
                return Math.max(metrics.stringWidth(name), metrics.stringWidth(preferredValue)) + JBUI.scale(20);
            }
        };
    }
}
