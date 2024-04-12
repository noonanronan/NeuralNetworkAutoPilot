package ie.atu.sw;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.Timer;

public class GameView extends JPanel implements ActionListener {
	// Some constants
	private static final long serialVersionUID = 1L;
	private static final int MODEL_WIDTH = 30;
	private static final int MODEL_HEIGHT = 20;
	private static final int SCALING_FACTOR = 30;

	private static final int MIN_TOP = 2;
	private static final int MIN_BOTTOM = 18;
	private static final int PLAYER_COLUMN = 15;
	private static final int TIMER_INTERVAL = 100;

	private static final byte ONE_SET = 1;
	private static final byte ZERO_SET = 0;

	private LinkedList<double[]> predictionQueue = new LinkedList<>();
	private static final int SMOOTHING_WINDOW = 2; // Number of predictions to consider for smoothing
	private static final double DECISION_THRESHOLD = 0.7; // Confidence threshold

	/*
	 * The 30x20 game grid is implemented using a linked list of 30 elements, where
	 * each element contains a byte[] of size 20.
	 */
	private LinkedList<byte[]> model = new LinkedList<>();

	// These two variables are used by the cavern generator.
	private int prevTop = MIN_TOP;
	private int prevBot = MIN_BOTTOM;

	// Once the timer stops, the game is over
	private Timer timer;
	private long time;

	private int playerRow = 11;
	private int index = MODEL_WIDTH - 1; // Start generating at the end
	private Dimension dim;

	// Some fonts for the UI display
	private Font font = new Font("Dialog", Font.BOLD, 50);
	private Font over = new Font("Dialog", Font.BOLD, 100);

	// The player and a sprite for an exploding plane
	private Sprite sprite;
	private Sprite dyingSprite;

	private boolean auto;

	// List to hold training data
	private List<double[]> trainingDataInputs = new ArrayList<>();
	private List<double[]> trainingDataOutputs = new ArrayList<>();

	private boolean isCollectingData = false; // Data collection flag

	private NeuralNetworkAutopilot autopilot;

	/**
     * Constructor for the game view.
     *
     * @param auto Boolean indicating whether autopilot is enabled.
     * @throws Exception Throws exception if initialization fails.
     */
	public GameView(boolean auto) throws Exception {
		this.auto = auto; // Use the autopilot
		setBackground(Color.LIGHT_GRAY);
		setDoubleBuffered(true);

		// Creates a viewing area of 900 x 600 pixels
		dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);

		initModel();
		timer = new Timer(TIMER_INTERVAL, this); // Timer calls actionPerformed() every second
		timer.start();

		if (this.auto) {
			initAutopilot(); // Initialize the autopilot when auto mode is enabled
		}
		System.out.println("Game initialized with autopilot mode: " + auto);
	}

	// Build our game grid
	private void initModel() {
		for (int i = 0; i < MODEL_WIDTH; i++) {
			model.add(new byte[MODEL_HEIGHT]);
		}
	}

	/**
     * Sets the sprite for the player.
     *
     * @param s The player sprite.
     */
	public void setSprite(Sprite s) {
		this.sprite = s;
	}

	/**
     * Sets the sprite for the dying animation.
     *
     * @param s The dying sprite.
     */
	public void setDyingSprite(Sprite s) {
		this.dyingSprite = s;
	}

	/**
     * Toggles data collection for training data.
     */
	public void toggleDataCollection() {
		isCollectingData = !isCollectingData;
		if (isCollectingData) {
			System.out.println("Data collection started.");
		} else {
			System.out.println("Data collection stopped.");
			System.out.println("Data collected. Input size: " + trainingDataInputs.size() + ", Output size: "
					+ trainingDataOutputs.size());
			saveTrainingData(); // Call this to handle the data when stopping collection
		}
	}

	/**
     * Extracts features from the game state.
     *
     * @return Extracted features.
     */
	public double[] extractFeatures() {
	    double[] features = new double[3]; // Simplified to one column ahead and playerRow
	    
	    byte[] columnAhead = model.get((PLAYER_COLUMN + 1) % MODEL_WIDTH);
	    features[0] = countFreeSpacesAbove(columnAhead);
	    features[1] = countFreeSpacesBelow(columnAhead);
	    features[2] = playerRow / (double) MODEL_HEIGHT; // Normalize playerRow

	    System.out.println("Extracted features: " + Arrays.toString(features));
	    return features;
	}

	
	/**
     * Counts free spaces above the player's current position.
     *
     * @param column The game column to inspect.
     * @return Normalized count of free spaces.
     */
	private double countFreeSpacesAbove(byte[] column) {
	    int count = 0;
	    for (int y = playerRow; y >= 0; y--) { // Start from playerRow and move up
	        if (column[y] == ZERO_SET) count++;
	        else break;
	    }
	    return count / (double) MODEL_HEIGHT; // Normalize
	}


	/**
     * Counts free spaces below the player's current position.
     *
     * @param column The game column to inspect.
     * @return Normalized count of free spaces.
     */
	private double countFreeSpacesBelow(byte[] column) {
	    int count = 0;
	    for (int y = playerRow; y < MODEL_HEIGHT; y++) { // Start from playerRow and move down
	        if (column[y] == ZERO_SET) count++;
	        else break;
	    }
	    return count / (double) MODEL_HEIGHT; // Normalize
	}

	/**
     * Records a move and its associated game state for training data.
     *
     * @param gameFeatures Features of the game state.
     * @param move The move made by the player.
     */
	public void recordMove(double[] gameFeatures, int move) {
        // Translation of move to actionIndex remains the same

        // Directly use gameFeatures without normalization
        double[] output = new double[3];
        output[move + 1] = 1.0; // Adjust for the action taken

        trainingDataInputs.add(gameFeatures); // Add raw features
        trainingDataOutputs.add(output); // Add output

        // Debug: Print the recorded move
        System.out.println("Recorded move: " + Arrays.toString(gameFeatures) + " -> " + Arrays.toString(output));

    }

    /**
     * Saves the collected training data to the file.
     */
	public void saveTrainingData() {
		try (FileWriter writer = new FileWriter("training_data.txt", true)) { // Append mode
			for (int i = 0; i < trainingDataInputs.size(); i++) {
				double[] inputs = trainingDataInputs.get(i);
				double[] outputs = trainingDataOutputs.get(i);

				// Convert the arrays to strings with proper formatting
				String inputStr = Arrays.stream(inputs).mapToObj(Double::toString).collect(Collectors.joining(",")); // Join
																														// with
																														// commas
				String outputStr = Arrays.stream(outputs).mapToObj(Double::toString).collect(Collectors.joining(",")); // Join
																														// with
																														// commas

				// Write to file, format: input:output
				writer.write(inputStr + ":" + outputStr + "\n");
			}
			writer.flush();
			// Clear the training data lists after saving to prevent data duplication
			trainingDataInputs.clear();
			trainingDataOutputs.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	 /**
     * Initializes the neural network autopilot.
     */
	private void initAutopilot() {
		autopilot = new NeuralNetworkAutopilot();
		// Assuming you have a method to load the trained network
		autopilot.loadTrainedNetwork("model.eg");
		System.out.println("Autopilot initialized.");
	}

	/**
     * Predicts the next move based on neural network output.
     *
     * @param features Features of the current game state.
     * @return The predicted move.
     */
	private int predictMove(double[] normFeatures) {
	    double[] output = autopilot.predict(normFeatures);
	    predictionQueue.offer(output);
	    System.out.println("P" + predictionQueue);
	    if (predictionQueue.size() > SMOOTHING_WINDOW) {
	        predictionQueue.poll();
	    }

	    double[] smoothedPrediction = new double[output.length];
	    for (double[] pred : predictionQueue) {
	        for (int i = 0; i < pred.length; i++) {
	            smoothedPrediction[i] += pred[i] / predictionQueue.size();
	            System.out.println(pred[i]);
	        }
	    }

	    System.out.println("Smoothed NN output: " + Arrays.toString(smoothedPrediction));

	    int moveIndex = getMaxIndex(smoothedPrediction);
	    System.out.println("Chosen move index: " + moveIndex);

	    if (smoothedPrediction[moveIndex] < DECISION_THRESHOLD) {
	        System.out.println("Predicted move: Stay with confidence: " + smoothedPrediction[moveIndex]);
	        return 0; // Stay if below confidence threshold
	    } else {
	        int move = moveIndex - 1; // Maps 0->-1 (Up), 1->0 (Stay), 2->1 (Down)
	        System.out.printf("Predicted move: %s with confidence: %f\n", move == 0 ? "Stay" : (move == -1 ? "Up" : "Down"), smoothedPrediction[moveIndex]);
	        return move;
	    }
	}


	/**
     * Finds the index of the maximum value in an array.
     *
     * @param array The array to search.
     * @return Index of the maximum value.
     */
	private int getMaxIndex(double[] array) {
		int maxIndex = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] > array[maxIndex]) {
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	/**
     * Repaints the game view.
     *
     * @param g The graphics object used for drawing.
     */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		var g2 = (Graphics2D) g;

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, dim.width, dim.height);

		int x1 = 0, y1 = 0;
		for (int x = 0; x < MODEL_WIDTH; x++) {
			for (int y = 0; y < MODEL_HEIGHT; y++) {
				x1 = x * SCALING_FACTOR;
				y1 = y * SCALING_FACTOR;

				if (model.get(x)[y] != 0) {
					if (y == playerRow && x == PLAYER_COLUMN) {
						timer.stop(); // Crash...
					}
					g2.setColor(Color.BLACK);
					g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
				}

				if (x == PLAYER_COLUMN && y == playerRow) {
					if (timer.isRunning()) {
						g2.drawImage(sprite.getNext(), x1, y1, null);
					} else {
						g2.drawImage(dyingSprite.getNext(), x1, y1, null);
					}

				}
			}
		}

		/*
		 * Not pretty, but good enough for this project... The compiler will tidy up and
		 * optimise all of the arithmetics with constants below.
		 */
		g2.setFont(font);
		g2.setColor(Color.RED);
		g2.fillRect(1 * SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
		g2.setColor(Color.WHITE);
		g2.drawString("Time: " + (int) (time * (TIMER_INTERVAL / 1000.0d)) + "s", 1 * SCALING_FACTOR + 10,
				(15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));

		if (!timer.isRunning()) {
			g2.setFont(over);
			g2.setColor(Color.RED);
			g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2 * SCALING_FACTOR);
		}
	}

	/**
     * Moves the player and records the action if data collection is active.
     *
     * @param step Direction and magnitude of the move.
     */
	public void move(int step) {
        playerRow += step;
        playerRow = Math.max(0, Math.min(playerRow, MODEL_HEIGHT - 1)); // Ensure playerRow is within bounds

        if (isCollectingData) {
            double[] gameFeatures = extractFeatures(); // Extract features directly without normalization
            recordMove(gameFeatures, step); // Record move with raw features
        }
    }

	/**
     * Autopilot movement logic, predicting and executing moves based on neural network output.
     */
	private void autoMove() {
		// Check if 1.5 seconds have elapsed since the game started
	    if (time * TIMER_INTERVAL < 1500) { // time is in ticks, TIMER_INTERVAL is the time per tick in milliseconds
	        return; // Do not make any move if less than 1.5 seconds have passed
	    }
		
		double[] gameFeatures = extractFeatures(); // This now includes the distance to next obstacle
		// Normalize features, which now include the new feature
	    int predictedAction = predictMove(gameFeatures); // Predicts -1, 0, or 1 based on four features

	    // Debugging output
	    System.out.println("Predicted action: " + predictedAction);

	    move(predictedAction); // Apply the move based on prediction
	}


	public boolean isAuto() {
		return auto;
	}

	/**
     * Handles game logic and rendering at each timer tick.
     *
     * @param e ActionEvent from the timer.
     */
	public void actionPerformed(ActionEvent e) {
		time++; // Update our timer
//		if (time%20 == 0)
//		{
//			double[] gameFeatures = extractFeatures();
//	        recordMove(gameFeatures, 0);
//		}
		
//		System.out.println("Game tick: " + time);
		this.repaint(); // Repaint the cavern

		// Update the next index to generate
		index++;
		index = (index == MODEL_WIDTH) ? 0 : index;

		generateNext(); // Generate the next part of the cave
		if (auto)
			autoMove();

		 
		if (time % 10 == 0) {
			/*
			 * double[] trainingRow = sample();
			 * System.out.println(Arrays.toString(trainingRow));
			 */
		}
	}

	/*
	 * Generate the next layer of the cavern. Use the linked list to move the
	 * current head element to the tail and then randomly decide whether to increase
	 * or decrease the cavern.
	 */
	private void generateNext() {
		var next = model.pollFirst();
		model.addLast(next); // Move the head to the tail
		Arrays.fill(next, ONE_SET); // Fill everything in

		// Flip a coin to determine if we could grow or shrink the cave
		var minspace = 4; // Smaller values will create a cave with smaller spaces
		prevTop += current().nextBoolean() ? 1 : -1;
		prevBot += current().nextBoolean() ? 1 : -1;
		prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace));
		prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

		// Fill in the array with the carved area
		Arrays.fill(next, prevTop, prevBot, ZERO_SET);
	}

	public double[] sample() {
		var vector = new double[MODEL_WIDTH * MODEL_HEIGHT];
		var index = 0;

		for (byte[] bm : model) {
			for (byte b : bm) {
				vector[index] = b;
				index++;
			}
		}
		return vector;
	}

	/**
     * Resets the game to its initial state.
     */
	public void reset() {
		model.stream() // Zero out the grid
				.forEach(n -> Arrays.fill(n, 0, n.length, ZERO_SET));
		playerRow = 11; // Centre the plane
		time = 0; // Reset the clock
		timer.restart(); // Start the animation
	}

}