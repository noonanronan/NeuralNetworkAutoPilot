
package ie.atu.sw;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

public class GameWindow implements KeyListener{
	private GameView view;
	
	public GameWindow() throws Exception {
		view = new GameView(true); //Use true to get the plane to fly in autopilot mode...
		init();
		loadSprites();
	}

	
	/*
	 * Build and display the GUI. 
	 */
	public void init() throws Exception {
	 	var f = new JFrame("ATU - B.Sc. in Software Development");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.addKeyListener(this);
        f.getContentPane().setLayout(new FlowLayout());
        f.add(view);
        f.setSize(1000,1000);
        f.setLocation(100,100);
        f.pack();
        f.setVisible(true);
	}
	
	
	/*
	 * Load the sprite graphics from the image directory
	 */
	public void loadSprites() throws Exception {
		var player = new Sprite("Player", 2,  "images/0.png", "images/1.png");
		view.setSprite(player);
		
		var explosion = new Sprite("Explosion", 7,  "images/2.png", 
				"images/3.png", "images/4.png", "images/5.png", 
				"images/6.png", "images/7.png", "images/8.png");
		view.setDyingSprite(explosion);
	}
	
	
	/*
	 * KEYBOARD OPTIONS
	 * ----------------
	 * UP Arrow Key: 	Moves plane up
	 * DOWN Arrow Key: 	Moves plane down
	 * S:				Resets and restarts the game
	 * 
	 * Maybe consider adding options for "start sampling" and "end
	 * sampling"
	 * 
	 */
	public void keyPressed(KeyEvent e) {
	    switch(e.getKeyCode()) {
	        case KeyEvent.VK_S: // Reset the game
	            view.reset();
	            break;
	        case KeyEvent.VK_D: // Toggle data collection
	            view.toggleDataCollection();
	            break;
	        case KeyEvent.VK_UP: // Move up
	            view.move(-1); // Move up
	            break;
	        case KeyEvent.VK_DOWN: // Move down
	            view.move(1); // Move down
	            break;
//	        case KeyEvent.VK_SPACE: // Spacebar pressed
//	            view.move(0); // Assign step = 0, indicating a stay action
//	            break;
	    }
	}


    public void keyReleased(KeyEvent e) {} 		//Ignore
	public void keyTyped(KeyEvent e) {} 		//Ignore
} 
