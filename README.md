# NeuralNetworkAutoPilot
4th Year Neural Network project

# Teaching a Neural Network to Fly Autopilot
**B.Sc. Software Development - Artificial Intelligence Assignment 2024**
## Overview
This project develops a neural network-based autopilot to navigate a plane in a scrolling tunnel game, aiming for a 30-second survival time. It outlines the methodologies and components used.

## Feature Engineering & Training Data
The project's neural network was trained using three key features: the plane's Y-axis position, free spaces above, and free spaces below in the next column. Data was collected in real-time, ensuring a diverse dataset that mirrors various gameplay scenarios. This method involved recording the game's state and the plane's movements, providing a robust foundation for the network to learn and autonomously navigate through the tunnel.

## Neural Network Architecture
The neural network is designed with simplicity and efficiency in mind, suitable for real-time control tasks. It consists of:
*Input Layer: 3 nodes, capturing the plane's position and obstacle proximity.
*Hidden Layers: Two layers, the first with 64 nodes and the second with 32 nodes, employing ReLU activation functions for non-linear transformation.
*Output Layer: 3 nodes, utilizing a SoftMax activation function to classify the plane's next move (up, stay, or down).

## Training and Performance
The network was trained using a dataset derived from manual gameplay, employing resilient propagation with improvement strategy adjustments. The training goal was achieved in under a minute, showcasing the network's ability to learn effective navigation strategies swiftly.

## Outcomes
The autopilot system successfully navigated the scrolling tunnel for periods exceeding the 30-second mark, demonstrating the network's robustness and the effectiveness of the chosen features and architecture. While performance varied due to the dynamic nature of the game it could fail sometimes it still managed to exceed 30 second a mostly.

## Conclusion
This project highlights the potential of neural networks in real-time control and decision-making tasks. Through careful feature selection, architecture design, and integration with the game environment, the developed system represents a successful application of artificial intelligence in autonomous navigation challenges.
Running The Jar
java -cp ".;C:\Users\ronan\Downloads\encog-core-3.4.jar;C:\Users\ronan\OneDrive - Atlantic TU\Year Four\Sem2\Artificial Intelligence\MainAssignement\Ai2024Assignement\bin" ie.atu.sw.Runner
Make sure to have the encog path added and the jar path.

