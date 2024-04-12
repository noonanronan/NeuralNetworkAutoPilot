package ie.atu.sw;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import org.encog.engine.network.activation.ActivationReLU;
import org.encog.engine.network.activation.ActivationSoftMax;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.ml.train.strategy.RequiredImprovementStrategy;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

/**
 * Manages the creation, training, and utilization of a neural network
 * for autopilot in a game scenario.
 */

public class NeuralNetworkAutopilot {

	private BasicNetwork network;
	
	/**
     * Creates the neural network with a specified architecture.
     */

	public void createNetwork() {
	    network = new BasicNetwork();
	    // Input layer with 4 inputs. No activation function needed as it's the input layer.
	    network.addLayer(new BasicLayer(null, true, 3));

	    // First hidden layer with more neurons to capture complex patterns. Using LeakyReLU for better gradient flow.
	    network.addLayer(new BasicLayer(new ActivationReLU(), true, 64));

	    // Adding an additional hidden layer can help model complexity, using fewer neurons to consolidate learning.
	    network.addLayer(new BasicLayer(new ActivationReLU(), true, 32));

	    // Second hidden layer as a refinement layer before the output, again using LeakyReLU.
	    network.addLayer(new BasicLayer(new ActivationReLU(), true, 16));

	    // Output layer with 3 outputs. SoftMax is suitable for classification where outputs represent probabilities.
	    network.addLayer(new BasicLayer(new ActivationSoftMax(), false, 3));

	    network.getStructure().finalizeStructure();
	    network.reset();
	}


	/**
     * Trains the network using the given training and validation datasets.
     *
     * @param trainingSet   The training dataset.
     * @param validationSet The validation dataset.
     */

	public void trainNetwork(MLDataSet trainingSet, MLDataSet validationSet) {
        MLTrain train = new ResilientPropagation(network, trainingSet);
        train.addStrategy(new RequiredImprovementStrategy(500));

        final int maxEpochs = 10000; // Maximum number of epochs for training
        double bestValidationError = Double.POSITIVE_INFINITY;
        int patience = 20; // Patience for early stopping
        int patienceCounter = 0; // Counter for epochs without validation error improvement

        for (int epoch = 1; epoch <= maxEpochs && patienceCounter < patience; epoch++) {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());

            double validationError = network.calculateError(validationSet);
            System.out.println("Validation Error: " + validationError);

            if (validationError < bestValidationError) {
                bestValidationError = validationError;
                patienceCounter = 0; // Reset counter if validation error improved
            } else {
                patienceCounter++; // Increment counter if no improvement
            }
        }
        train.finishTraining();

        if (patienceCounter >= patience) {
            System.out.println("Early stopping triggered after " + patience + " epochs without improvement.");
        }

        // Consider saving the model here if it's the best one
    }

	

	public BasicMLDataSet loadTrainingData(String filePath) {
		ArrayList<double[]> inputData = new ArrayList<>();
		ArrayList<double[]> idealData = new ArrayList<>();

		try (Scanner scanner = new Scanner(new File(filePath))) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split(":");
				double[] inputs = Arrays.stream(parts[0].split(",")).mapToDouble(Double::parseDouble).toArray();
				double[] outputs = Arrays.stream(parts[1].split(",")).mapToDouble(Double::parseDouble).toArray();
				inputData.add(inputs);
				idealData.add(outputs);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		double[][] inputArray = inputData.toArray(new double[0][]);
		double[][] idealArray = idealData.toArray(new double[0][]);
		return new BasicMLDataSet(inputArray, idealArray);
	}

	/**
     * Splits the dataset into training and validation sets based on a given ratio.
     *
     * @param fullDataSet   The full dataset to split.
     * @param trainingRatio The ratio of data to use for training.
     * @return An array containing the training and validation datasets.
     */
	public MLDataSet[] splitData(BasicMLDataSet fullDataSet, double trainingRatio) {
	    int totalSize = fullDataSet.size();
	    int trainingSize = (int) (totalSize * trainingRatio);
	    
	    ArrayList<double[]> trainingInput = new ArrayList<>();
	    ArrayList<double[]> trainingIdeal = new ArrayList<>();
	    ArrayList<double[]> validationInput = new ArrayList<>();
	    ArrayList<double[]> validationIdeal = new ArrayList<>();
	    
	    for (int i = 0; i < trainingSize; i++) {
	        trainingInput.add(fullDataSet.get(i).getInputArray());
	        trainingIdeal.add(fullDataSet.get(i).getIdealArray());
	    }
	    
	    for (int i = trainingSize; i < totalSize; i++) {
	        validationInput.add(fullDataSet.get(i).getInputArray());
	        validationIdeal.add(fullDataSet.get(i).getIdealArray());
	    }
	    
	    BasicMLDataSet trainingSet = new BasicMLDataSet(trainingInput.toArray(new double[0][]), trainingIdeal.toArray(new double[0][]));
	    BasicMLDataSet validationSet = new BasicMLDataSet(validationInput.toArray(new double[0][]), validationIdeal.toArray(new double[0][]));
	    
	    return new MLDataSet[]{trainingSet, validationSet};
	}

	
	public void setupAndTrain() {
	    // Load training data from file
	    MLDataSet[] splitSets = splitData(loadTrainingData("training_data.txt"), 0.8); // Only pass the training ratio
	    MLDataSet trainingSet = splitSets[0];
	    MLDataSet validationSet = splitSets[1];

	    // Create and initialize the network structure
	    createNetwork();

	    // Train the network with the loaded data
	    trainNetwork(trainingSet, validationSet);
	    
	    saveModel("model.eg");
	    System.out.println("Network trained and model saved.");
	}
	
	/**
     * Saves the trained model to a specified path.
     *
     * @param path The path where the model should be saved.
     */
	
	public void saveModel(String path) {
        File file = new File(path);
        EncogDirectoryPersistence.saveObject(file, network);
        
        System.out.println("Model saved to: " + path);
	}
	
	/**
     * Loads a trained network from a specified file path.
     *
     * @param path The path to the model file.
     */
	
	public void loadTrainedNetwork(String path) {
	    File file = new File(path);
	    if (!file.exists()) {
	        System.err.println("Model file does not exist: " + path);
	        // You could initialize your network here if appropriate
	        return;
	    }
	    try {
	        network = (BasicNetwork) EncogDirectoryPersistence.loadObject(file);
	        System.out.println("Model loaded from: " + path);
	    } catch (Exception e) {
	        e.printStackTrace();
	        System.err.println("Error loading the neural network from path: " + path);
	    }
	}
	
	/**
     * Predicts the next move based on input game features.
     *
     * @param gameFeatures The features of the game's current state.
     * @return The predicted move.
     */
	public double[] predict(double[] gameFeatures) {
	    if (this.network == null) {
	        // Handle the case where the network hasn't been loaded or initialized
	        System.err.println("Network is not initialized.");
	        return new double[0]; // Return an empty array or some error indicator
	    }
	    MLData input = new BasicMLData(gameFeatures);
	    MLData output = network.compute(input);
	    return output.getData();
	}




	/* Test to see if it will print out first 5 data points */
//	public void testDataLoading() {
//        BasicMLDataSet dataSet = loadTrainingData("training_data.txt");
//        
//        // Just print the first 5 data points
//        int dataPointCount = 0;
//        for(MLDataPair pair : dataSet) {
//            System.out.println("Input: " + Arrays.toString(pair.getInputArray()));
//            System.out.println("Ideal: " + Arrays.toString(pair.getIdealArray()));
//            dataPointCount++;
//            if(dataPointCount >= 5) break;
//        }
//    }

	public static void main(String[] args) {
		NeuralNetworkAutopilot autopilot = new NeuralNetworkAutopilot();
		autopilot.setupAndTrain();
	}

}