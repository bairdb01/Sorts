package gui;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Benjamin Baird
 * Date Last Updated: Sept 27, 2016
 * Contains all the sorting algorithms used
 */
public class Sorter implements Runnable {
    private final Integer [] randNumSet;
    private final String sortAlg;
    private final boolean step;
    private static int counter;
    private final BlockingQueue <Integer[]> q;
    private final int maxStepCount;
    private final ReentrantLock lock;

    public Sorter(Integer [] randNumSet, String sortAlg, boolean step, int maxStepCount,ReentrantLock lock, BlockingQueue <Integer[]> q ){
        this.randNumSet = randNumSet.clone();
        this.sortAlg = sortAlg;
        this.step = step;
        this.lock = lock;
        Sorter.counter = 0;
        this.q = q;
        this.maxStepCount = maxStepCount;
    }

    /**
     * Compares with previous number and checks if it is > than or < than and positions itself in ascending order
     * @param randomNums random numbers to be sorted
     * @param step step through or fully sort
     */
    private void insertionSort(Integer randomNums[], boolean step){
        Integer numSet[]= randomNums.clone();
        int curNum;
        int stepCount = 1;

        //Loop through all the numbers
        for (int i = 1; i < numSet.length; i++){
            // Stepwise pause
            if (step && stepCount % (numSet.length/maxStepCount) == 0){
                suspendStep(numSet);
            }

            // Figure out where to insert the number and shift numbers
            int j = i;
            while ( j > 0 && (numSet[j-1] > numSet[j])) {
                curNum = numSet[j];
                numSet[j] = numSet[j-1];
                numSet[j - 1] = curNum;
                j--;
            }
            stepCount++;
        }
        addToQ(numSet);
    }

    /**
     * Go through array, largest number "floats" to the last slot, repeat but with size - 1
     * @param randomNums random
     * @param step step through sort or fully sort
     */
    private void bubbleSort(Integer randomNums[], boolean step) {
        Integer numSet[] = randomNums.clone();
        int stepCount = 0;
        boolean swapped = true;


        while (swapped) {
            swapped = false;

            // Don't need to check the last item, since we know it is largest
            for (int i = 1; i < numSet.length - stepCount; i++) {
                if (numSet[i] < numSet[i-1]){
                    // Swap
                    Integer temp = numSet[i];
                    numSet[i] = numSet[i - 1];
                    numSet[i - 1] = temp;
                    swapped = true;
                }
            }
            stepCount++;

            // Stepwise pause
            if (step && stepCount % (numSet.length/maxStepCount) == 0){
                suspendStep(numSet);
            }
        }
        addToQ(numSet);
    }


    /**
     * Repairs a heap; can be used for sub trees
     * @param heap the main heap
     * @param start the position of the root node
     * @param end the end of the heap
     * @return modified heap
     */
    private Integer[] siftDown(Integer heap[], int start, int end){
        int root = start;
        while((2 * root + 1) <= end) {
            int child = 2 * root + 1; // Left Child
            int swap = root;
            if (heap[swap] < heap[child]) {
                swap = child;
            }

            if ((child + 1) <= end && heap[swap] < heap[child+1]){
                swap = child + 1;
            }

            if (swap == root) {
                return heap;
            } else {
                Integer temp = heap[root];
                heap[root] = heap[swap];
                heap[swap] = temp;
                root = swap;
            }
        }
        return heap;
    }

    /**
     * Puts the input array into a max-heap
     * @param heap randomly assorted integer array
     * @return Heapified max-heap
     */
    private Integer[] heapify(Integer heap[]) {
        int start = (heap.length-1)/2;
        while (start >= 0){

            heap = siftDown(heap,start, heap.length - 1);
            start--;
        }

        return heap;
    }

    /**
     * Moves the root of the heap to the last element until all items have been processed
     * @param randomNums random numbers to be sorted
     * @param step step through sort
     */
    private void heapSort(Integer randomNums[], boolean step){
        Integer heap[] = heapify(randomNums);
        int end = heap.length - 1;
        int stepCount = 1;
        while (end > 0) {
            if (step && stepCount % (heap.length/maxStepCount) == 0){
                suspendStep(heap);
            }
            Integer temp = heap[end];
            heap[end] = heap[0];
            heap[0] = temp;
            end--;
            heap = siftDown(heap, 0, end);
            stepCount++;
        }
        addToQ(heap);
    }

    /**
     * Merge the workset into the dataset
     * @param dataSet the finalized set
     * @param begin starting position
     * @param middle middle position
     * @param end end position
     * @param workSet work array with data that needs to be transfered to the dataSet
     */
    private void mergeSortMerge(Integer dataSet[], int begin, int middle, int end, Integer workSet[]){
        int i = begin, j = middle;
        for (int k= begin; k < end; k++) {
            if (i < middle && (j >= end || dataSet[i] <= dataSet[j])) {
                workSet[k] = dataSet[i];
                i++;
            } else {
                workSet[k] = dataSet[j];
                j++;
            }
        }
    }

    /**
     * Splits the dataset in half
     * @param dataSet data to split
     * @param begin start position in dataset
     * @param end index of last element
     * @param workSet an array to work in
     */
    private void mergeSortSplit(Integer dataSet[], boolean step, int begin, int end, Integer workSet[]){
        if (end - begin < 2)
            return;

        // Split runs in half
        int middle = (end + begin)/2;
        mergeSortSplit(dataSet, step, begin, middle, workSet);
        mergeSortSplit(dataSet, step, middle, end, workSet);
        mergeSortMerge(dataSet, begin, middle, end, workSet);
        Sorter.counter++;

        if (step && Sorter.counter %  (workSet.length / 20) == 0) {
            suspendStep(dataSet);
        }

        System.arraycopy(workSet, begin, dataSet, begin, end - begin);
    }

    /**
     * Begins the merge sort process
     * @param randomNums numbers to be sorted
     * @param step step through sort
     */
    private void mergeSort(Integer randomNums[], boolean step){
        Sorter.counter = 0;
        Integer work[] = new Integer[randomNums.length];
        Integer numSet[] = randomNums.clone();
        mergeSortSplit(numSet, step, 0, randomNums.length, work);
        addToQ(numSet);
    }

    /**
     * Finds a number to pivot on
     * @param dataSet numbers to sort
     * @param low start of array
     * @param high end of array
     * @return pivot index
     */
    private int partition (Integer dataSet[], int low, int high){
        int pivot = dataSet[high];
        int i = low;
        for ( int j = low; j <= high - 1; j ++) {
            if (dataSet[j] <= pivot) {
                Integer temp = dataSet[i];
                dataSet[i] = dataSet[j];
                dataSet[j] = temp;
                i++;
            }
        }
        Integer temp = dataSet[i];
        dataSet[i] = dataSet[high];
        dataSet[high] = temp;

        return i;
    }

    /**
     * Recursively finds a pivot point to sort the data
     * @param dataSet numbers to be sorted
     * @param low start index
     * @param high end index
     * @param step step through sort or fully sort
     */
    private void quickSort(Integer dataSet[], int low, int high, boolean step) {
        if (low < high) {
            int p = partition(dataSet, low, high);
            quickSort(dataSet, low, p - 1, step);
            quickSort(dataSet, p + 1, high, step);
            Sorter.counter++;

            if (step && Sorter.counter % (dataSet.length/maxStepCount) == 0){
                suspendStep(dataSet);
            }
        }
    }

    /**
     * Intermediate step to print the sorted numbers and call the sorting method
     * @param randNum random number set
     * @param step pause every few steps or no pauses
     */
    private void quickSort (Integer randNum[], boolean step) {
        Integer numSet[] = randNum.clone();
        quickSort(numSet, 0, numSet.length - 1, step);
        addToQ(numSet);
    }

    /**
     * Sorts the numberset based on it's bucket position
     * @param numSet number set to be sorted
     * @param bucketCount number of digits in each category [0 - 9]
     * @param digit which digit we are one
     * @param radix what radix to use
     * @return sorted numbers in an integer array
     */
    private Integer [] radixSortJoin (Integer [] numSet, Integer [] bucketCount, int digit, int radix) {
        Integer [] sortedSet = new Integer[numSet.length];
        for (int i = 1; i < bucketCount.length; i ++) {
            bucketCount[i] += bucketCount[i-1];
        }
        for (int i = (numSet.length - 1); i >= 0; i--) {
            int bucketIndex = getRadixBucket(numSet[i], digit, radix);
            int sortedIndex = bucketCount[bucketIndex] - 1;
            sortedSet[sortedIndex] = numSet[i];
            bucketCount[bucketIndex]--;
        }
        return sortedSet;
    }
/*
    /**
     * Merges buckets from 0 - 9 into an array
     * @param buckets list of all the buckets
     * @param numSet array to store sorted buckets
     */
//    private void radixSortJoin(ArrayList<ArrayList<Integer>> buckets, Integer numSet[]){
//         // Rejoin
//        int index = -1;
//        for(ArrayList<Integer> bucket:
//                buckets){
//            for (Integer num:
//                    bucket) {
//                index++;
//                numSet[index] = num;
//            }
//        }
//    }

    /**
     * Returns the bucket index of a number given current digit position and radix
     * @param n number to index
     * @param digit current position in the number
     * @param radix which base to use
     * @return bucket index
     */
    private int getRadixBucket(int n, int digit, int radix) {
        return (n/digit) % radix;
    }

    /**
     * Sort from least significant digit to most (Right to left) maintaining order
     * @param randomNums numbers to be sorted
     * @param step step through or fully sort
     */
    private void radixSort(Integer randomNums[], boolean step){
        Integer numSet[] = randomNums.clone();
        ArrayList<ArrayList<Integer>> buckets = new ArrayList<>(10);
        Integer [] bucketCount = new Integer[10];

        // Initialize buckets
        for (int i = 0; i < 10; i ++)
            bucketCount[i] = 0;

        // Sort
        boolean maxLen = false;
        int digit = 1;
        int radix = 10;

        while (!maxLen) {
            // Group by LSD
            for (Integer x :
                    numSet) {
                int bucketNum = getRadixBucket(x, digit, radix);
                bucketCount[bucketNum]++;
            }


            if (digit == 100) {
                maxLen = true;
            } else {
                maxLen = false;
            }

            // Add numbers to array
            numSet = radixSortJoin(numSet, bucketCount, digit, radix);

            // Stepwise pause
            if (step) {
                suspendStep(numSet);
            }

            // Clear buckets
            for (int i = 0; i < 10; i++) {
                bucketCount[i] = 0;
            }

            digit *= radix;
        }
        addToQ(numSet);
    }



//
//        for (int i = 0; i < 10; i++)
//            buckets.add(new ArrayList<>());
//
//        // Sort
//        boolean maxLen = false;
//        int digit = 1;
//        int radix = 10;
//
//        while (!maxLen) {
//            // Group by LSD
//            for (Integer x :
//                    numSet) {
//                int bucketNum = (x/digit) % radix;
//
//                buckets.get(bucketNum).add(x);
//            }
//            if (numSet.length == buckets.get(0).size()) {
//                maxLen = true;
//                continue;
//            } else {
//                maxLen = false;
//            }
//
//            // Add buckets to array
//            radixSortJoin(buckets, numSet);
//
//            // Stepwise pause
//            if (step) {
//                suspendStep(numSet);
//            }
//
//            // Clear buckets
//            for (int i = 0; i < 10; i++) {
//                buckets.get(i).clear();
//            }
//
//            digit *= radix;
//        }

//        addToQ(numSet);
//    }

    /**
     * Prints an arraylist of integers to console
     * @param numSet the list of numbers to print
     */
    private void printNumList (Integer numSet[]) {
        for (int number:
                numSet) {
            System.out.println(number);
        }
    }

    /**
     * Calls the selected sort algorithm
     */
    private void sort(boolean step){
        // Execute search based on selected algorithm
        switch(this.sortAlg){
            case ("Insertion Sort"):
                insertionSort(randNumSet, step);
                break;
            case ("Heap Sort"):
                heapSort(randNumSet, step);
                break;
            case ("Bubble Sort"):
                bubbleSort(randNumSet, step);
                break;
            case ("Merge Sort"):
                mergeSort(randNumSet, step);
                break;
            case ("Quick Sort"):
                quickSort(randNumSet, step);
                break;
            case ("Radix Sort"):
                radixSort(randNumSet, step);
                break;
        }
    }

    private void addToQ(Integer[] numSet){
        try {
            q.put(numSet);
        } catch (Exception e) {
//            System.out.println("Sort Thread Interrupted");
        }
    }

    private void suspendStep(Integer [] numSet){
        addToQ(numSet);
        unLock();
//        System.out.println("Suspend");
        try {
            Thread.sleep(10);
        } catch (Exception e) {
//            System.out.println("Sort Thread Interrupted");
        }
        getLock();
//        System.out.println("Acquired SEM; WOKEUP");
    }


    private void getLock(){
        this.lock.lock();
    }

    private void unLock() {
        try {
            if (lock.isLocked())
                this.lock.unlock();
        } catch (Exception e) {
//            System.out.println("Sort Thread Interrupted");
        }
    }

    @Override
    public void run() {
        getLock();
        sort(this.step);
        unLock();
    }
}
