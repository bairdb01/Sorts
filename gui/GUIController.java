package gui;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

/**
 * @author Benjamin Baird
 * Last Updated: Sept 23, 2016
 * Description: Implements various sorting algorithms and provides a visual representation of their speeds
 **/
public class GUIController implements Initializable {
    private BlockingQueue <Integer[]> q;
    private Integer randNumSet[];
    private ReentrantLock lock;
    private ArrayList<Thread> threadList;
    private int stepCount;
    private static ChartController chartController;
    private final ObservableList<String> algNames = FXCollections.observableArrayList(
            "Insertion Sort",
            "Heap Sort",
            "Bubble Sort",
            "Merge Sort",
            "Quick Sort",
            "Radix Sort"
    );

    @FXML
    Label totalTimeLabel;

    @FXML
    Button stepBtn;

    @FXML
    private ComboBox<String> sortAlgBox;

    @FXML
    private Label timeLabel;

    @FXML
    private ScatterChart <Number, Number> chart;

    /**
     * Performs any clean up required when the algorithm selection has changed
     */
    public void comboSelected(){
        chartController.populateChart(randNumSet);
        stepBtn.setDisable(false);
        stepCount = 0;
        if (threadList.size() > 0) {
            while (threadList.size() > 0){
                threadList.get(0).interrupt();
                threadList.remove(0);
            }
        }
        q = new LinkedBlockingQueue<>();
        timeLabel.setText("");
        totalTimeLabel.setText("");
    }

    /**
     * Generate a random number from 0 to 999 and populates the number set
     **/
    @FXML
    protected void genNumbers() {
        Random ran = new Random();
        int numSetSize = ran.nextInt(600) + 400;
//        int numSetSize = 10;
        randNumSet = new Integer[numSetSize];
        for (int i = 0; i < numSetSize; i++) {
            randNumSet[i] = ran.nextInt(1001);
        }
        chartController.populateChart(randNumSet);
        stepBtn.setDisable(false);
        this.stepCount = 0;
        q = new LinkedBlockingQueue<>();
        timeLabel.setText("");
        totalTimeLabel.setText("");
    }

    /**
     * Initializes random numbers and any default values
     * @param location default values; unused
     * @param resources default values; unused
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortAlgBox.setItems(algNames);
        sortAlgBox.getSelectionModel().select(0);

        chartController = new ChartController(chart);
        genNumbers();

        this.lock = new ReentrantLock();
        this.q = new LinkedBlockingQueue<>();
        this.threadList = new ArrayList<>(1);
        this.stepCount = 0;
    }

    /**
     * Averages total time for the search function over 5 tries
     */
    public void time(){
        long totalTime = 0;
        for (int i = 0; i < 100; i++) {
            if (i == 99)
                totalTime += sort(false, true);
            totalTime += sort(false, false);
        }
        long avg = totalTime/100;
        timeLabel.setText("Average time over 100 trails: " + avg + "ns");
        totalTimeLabel.setText("Total Time for 100 trails: " + totalTime/1000000 +"ms");
    }

    /**
     * Calls the sort algorithm with step=true
     */
    @FXML
    protected void handleStep(){
        timeLabel.setText("");
        totalTimeLabel.setText("");
        sort(true, true);
    }

    /**
     * Starts the sorting algorithm thread
     * @param step true if wise to step through; false if just want it to sort
     * @return time it took to sort
     */
    private long sort(boolean step, boolean draw){
        int maxStepCount = 20;
        long endTime = 0, startTime = 0;

        // Kill off any previous threads
        if ((!step || ((stepCount == 0 || threadList.get(0).getState() == Thread.State.TERMINATED) && threadList.size() > 0))) {
            while (threadList.size() > 0){
                threadList.get(0).interrupt();
                threadList.remove(0);
            }
            q = new LinkedBlockingQueue<>();  // New q, bug fix; old one is contaminated
            stepCount = 0;
            stepBtn.setDisable(false);
        }

        try {
            if (lock.isLocked())
                lock.unlock();

            // Create a new instance of a sort
            if (stepCount == 0) {
                Sorter so = new Sorter(randNumSet, sortAlgBox.getValue(), step, maxStepCount, lock, q);
                threadList.add(new Thread(so));
                startTime = System.nanoTime();
                threadList.get(0).start();
            }

            // Disable step button if done sorting
            if (step) {
                if (stepCount > maxStepCount || (threadList.get(0).getState() == Thread.State.TERMINATED)) {
                    stepBtn.setDisable(true);
                }
                stepCount++;
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }

        try {
            // Wait for the sorting algorithm to complete, for timings
            if (!step)
                threadList.get(0).join();
            else
                sleep(10);
            lock.lock();
            endTime= System.nanoTime();

            // Update the chart
            Integer[] numSet = q.take();
            if (draw)
                chartController.populateChart(numSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (endTime - startTime);
    }
}
