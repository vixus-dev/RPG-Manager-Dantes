package br.com.dantesrpg.controller.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Grid que distribui seus filhos em colunas iguais e cria novas linhas quando a
 * largura disponível fica menor que a largura mínima desejada para uma coluna.
 */
public class ResponsiveGridPane extends GridPane {
	private int maxColumns = 3;
	private double minColumnWidth = 300;
	private int currentColumns = -1;
	private int currentChildrenCount = -1;

	public ResponsiveGridPane() {
		widthProperty().addListener((obs, oldWidth, newWidth) -> updateResponsiveLayout());
		getChildren().addListener((ListChangeListener<Node>) change -> updateResponsiveLayout());
	}

	public int getMaxColumns() {
		return maxColumns;
	}

	public void setMaxColumns(int maxColumns) {
		this.maxColumns = Math.max(1, maxColumns);
		invalidateResponsiveLayout();
	}

	public double getMinColumnWidth() {
		return minColumnWidth;
	}

	public void setMinColumnWidth(double minColumnWidth) {
		this.minColumnWidth = Math.max(1, minColumnWidth);
		invalidateResponsiveLayout();
	}

	public int getCurrentColumns() {
		updateResponsiveLayout();
		return currentColumns;
	}

	@Override
	protected void layoutChildren() {
		updateResponsiveLayout();
		super.layoutChildren();
	}

	private void invalidateResponsiveLayout() {
		currentColumns = -1;
		requestLayout();
	}

	private void updateResponsiveLayout() {
		double usableWidth = Math.max(0,
				getWidth() - snappedLeftInset() - snappedRightInset());
		double columnAndGap = minColumnWidth + getHgap();
		int calculatedColumns = usableWidth <= 0
				? maxColumns
				: (int) Math.floor((usableWidth + getHgap()) / columnAndGap);
		calculatedColumns = Math.max(1, Math.min(maxColumns, calculatedColumns));

		if (calculatedColumns == currentColumns
				&& currentChildrenCount == getChildren().size()) {
			return;
		}

		currentColumns = calculatedColumns;
		currentChildrenCount = getChildren().size();
		getColumnConstraints().clear();
		for (int column = 0; column < currentColumns; column++) {
			ColumnConstraints constraints = new ColumnConstraints();
			constraints.setMinWidth(0);
			constraints.setPercentWidth(100.0 / currentColumns);
			constraints.setHgrow(Priority.ALWAYS);
			constraints.setFillWidth(true);
			getColumnConstraints().add(constraints);
		}

		int index = 0;
		for (Node child : getChildren()) {
			setColumnIndex(child, index % currentColumns);
			setRowIndex(child, index / currentColumns);
			setHgrow(child, Priority.ALWAYS);
			setVgrow(child, Priority.ALWAYS);
			setFillWidth(child, true);
			setFillHeight(child, true);
			if (child instanceof Region region) {
				region.setMinWidth(0);
			}
			index++;
		}
	}
}
