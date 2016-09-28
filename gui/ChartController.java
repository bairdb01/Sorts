package gui;

import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

/**
 * Contains the controller for the scatter graph
 * @author Ben Baird
 */
class ChartController {

    private static ScatterChart<Number, Number> chart;

    ChartController(ScatterChart<Number, Number> chart) {
        ChartController.chart = chart;
    }

    /**
     * Updates the chart with data
     */
    void populateChart(Integer numSet[]){
        if (chart.getData().size() > 0)
            chart.getData().remove(0);
        XYChart.Series <Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < numSet.length; i++)
            series.getData().add(new XYChart.Data<>(i, numSet[i]));
        chart.getData().addAll(series);
    }
}
