import acm.graphics.*;
import acm.util.RandomGenerator;
import java.util.Arrays;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import com.shpp.cs.a.graphics.WindowProgram;

public class Breakout extends WindowProgram {
	private static final long serialVersionUID = 1L;
	
	/* STATIC CONSTANTS */
	public static final int APPLICATION_WIDTH = 400;
    public static final int APPLICATION_HEIGHT = 600;
    
 	/* Approximate calculations of the minimum and maximum width and height of the user's screen */ 
 	private static final int MIN_SCREEN_WIDTH = 320;
 	private static final int MAX_SCRREN_WIDTH = 1440;
 	
 	/* limit values of the custom font size for notification text  */
 	private static final int MIN_FONT_SIZE = 16;
 	private static final int MAX_FONT_SIZE = 24;
 	
	/** Number of bricks per row */
	private static final int NBRICKS_PER_ROW = 10;
	/** Number of rows of bricks */
	private static final int BRICK_ROWS = 10;
	
	private static final int BRICK_SPACING = 1;
	
	/* constant is used to correct the indent between blocks (bricks) */
	private static final int COMPENSATORY_MARGIN = 1;
	
	private static final int BRICK_HEIGHT = 10;
	private static final int BRICK_Y_OFFSET = 70;

	private static final int PADDLE_WIDTH = 60;
	private static final int PADDLE_HEIGHT = 10;
	private static final double PADDLE_Y_OFFSET = 30;
	
    /** Number of turns */
    private static final int NTURNS = 3;
	
	private static final int LABEL_OFFSET = 30;
	 
	private static final Color ORANGE_COLOR = new Color(220, 105, 15);
	
	/* Radius of the ball in pixels */
	private static final int BALL_RADIUS = 8;
	private static final int BALL_DIAMETER = BALL_RADIUS * 2;
	
	private static final double REBOUND_RATIO = 1.1;
	
	/* values of the maximum allowable ball speed during the game */
	private static final double MAX_SPEED_BALL= 10.0;
	private static final double MIN_INITIAL_SPEED = 2.0;
	private static final double MAX_INITIAL_SPEED = 4.0;

	private static final double PAUSE_TIME = 1000.0 / 60;
	
	/* color for bricks */
	private static final Color[] ROW_COLOR = {
			Color.RED, Color.RED, 
			Color.ORANGE, Color.ORANGE, 
			Color.YELLOW, Color.YELLOW, 
			Color.GREEN, Color.GREEN, 
			Color.CYAN, Color.CYAN,
		};
	
	/* block collection */
	private static final GRect[] BRICKS = new GRect[BRICK_ROWS * NBRICKS_PER_ROW];

	/* Dynamically changeable objects and variables for evictions */
	GRect paddle = null;	
	GOval ball = null;
	
	/* are necessary to display the current status of the game play and notify the user  */ 
	GLabel scoreLabel = null;
	GLabel livesLabel = null;
	GLabel statusLabelNotification = null;
	
	/* In this case, this variable serves as insurance that 
	 * if the height of the screen is low, 
	 * and the ball in the initial position 
	 * will be at the intersection of the coordinates with the bricks, 
	 * we will not remove them until the first paddle stroke occurs.
	 */
	private boolean isRebound = false;
	
	/* variables are used to calculate the current state
	 * of the user screen and configure the state of the block width and font size
	 */
	double screenWidth;
	double screenHeight;
	double halfWidth; 
	double halfHeight;
	double fontSize;
	double brickWidth;

	/* the player's achievement at the time of the game session */
	private int score = 0;
	/* default value of player lives */
	private int lives = NTURNS;
	
	/* observe the number of remaining blocks during the game */
	private int bricksCounter = BRICKS.length;

	private double vx, vy;
	
	/* main method of program execution */
	public void run() {
		/* 
		 * calculate the current width and height of the screen
		 * for further use and configuration of elements relative to these values
		 */
		screenWidth = getWidth();
		screenHeight = getHeight();
		
		halfWidth = screenWidth / 2;
		halfHeight = screenHeight / 2;
		
		
		brickWidth = (screenWidth - (NBRICKS_PER_ROW - 1) * (BRICK_SPACING + COMPENSATORY_MARGIN)) / NBRICKS_PER_ROW;
		
		
		/* This calculation determines the size of the font relative to the screen size */
		fontSize = (screenWidth - MIN_SCREEN_WIDTH) / 
				   (MAX_SCRREN_WIDTH - MIN_SCREEN_WIDTH) * 
				   (MAX_FONT_SIZE - MIN_FONT_SIZE) + MIN_FONT_SIZE;
		
		/* process execution program */
		process();
	}

	private void process() {
		/* Creating user interface components to interact and display the result and */
		createLivesLabel();
		createScoreLabel();
		createPaddle(halfWidth - PADDLE_WIDTH / 2, screenHeight - PADDLE_WIDTH - PADDLE_Y_OFFSET);
		createBricks((int) brickWidth, BRICK_HEIGHT);
		statusNotification("Click and START GAME");
		
		/* set mouse listeners */
		addMouseListeners();	
		
		/* The game play begins after the user clicks on the game progress screen */
		waitForClick();
		
		startGame();	
	}
	
	/* 
	 * method that implements the dynamics of the game
	 * itself and manages the processes
	 * of behavior depending on the player's achievements
	 */
	private void startGame() {	
		createBall(halfWidth - BALL_RADIUS, halfHeight - BALL_RADIUS);
		setInitialRandomBallMovement(MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);
		
			while (lives > 0) {
				ball.move(vx, vy);
				
				GObject collider = getCollidingObject(ball);

				if (isBallNoCollisions()) {
					handleBallNoCollisions();
					continue;
				}

				if (collider == paddle) {
					reboundBall();
					continue;
				} 
		
				if (collider != null && isRebound && Arrays.asList(BRICKS).contains(collider)) {
					destroyBrick(collider);
					continue;
				}
				
				if (bricksCounter == 0) {
					handleWincCndition();
					break;
				} 
				
				pause(PAUSE_TIME);
			}
			
			resetGame();	
	}
	
	/* clear up UI element and set default value */
	private void resetGame() {
		destroyViewElement();
		removeBricks(BRICKS);
		createPaddle(halfWidth - PADDLE_WIDTH / 2, screenHeight - PADDLE_WIDTH - PADDLE_Y_OFFSET);
		createBricks((int) brickWidth, BRICK_HEIGHT);
		isRebound = false;
		lives = NTURNS;
		score = 0;
		bricksCounter = BRICKS.length;
		updateLivesLabel(3);
		updateScoreLabel(0);
		
		statusNotification("Let's try again? click if you want");
		
		waitForClick();
		remove(statusLabelNotification);
		startGame();
	}
	
	/* checking the state of knowledge of the game process and further branching of game events */
	private void handleBallNoCollisions() {
		updateLivesLabel(lives -= 1);
		
		if (lives > 0) restartGameSession();
		else {
			statusNotification("GAME OVER");
			pause(1000);
			remove(statusLabelNotification);
			resetGame();
		}
	}
	
	/* restarting the game play and starting the next round */
	private void restartGameSession() {
		statusNotification("Pass, you have " + lives + " shot at it.");
		waitForClick();
		isRebound = false;
		destroyViewElement();
		createPaddle(halfWidth - PADDLE_WIDTH / 2, screenHeight - PADDLE_WIDTH - PADDLE_Y_OFFSET);
		createBall(halfWidth - BALL_RADIUS, halfHeight - BALL_RADIUS);
		setInitialRandomBallMovement(MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);

	}
	
	/* notification of the user about the successful completion of the game */
	private void handleWincCndition() {
		statusNotification("WIN! your result: " + score + " score");	
		pause(1000);
		
		remove(statusLabelNotification);
	}

	/* CREATING GAME ELEMENTS */
	
	/**
	 * @param BRICK_WIDTH
	 * @param BRICK_HEIGHT
	 * @return void
	 * Creating a matrix of elements (paddle) and displaying it on the screen
	 */
	private void createBricks(int BRICK_WIDTH, int BRICK_HEIGHT) {
		int index = 0;
		double startX = (screenWidth - (NBRICKS_PER_ROW * (brickWidth + BRICK_SPACING + COMPENSATORY_MARGIN) - BRICK_SPACING)) / 2;
		
		for (int axisX = 0; axisX < NBRICKS_PER_ROW; axisX++) {
			for (int axisY = 0; axisY < BRICK_ROWS; axisY++) {
				GRect brick = new GRect(
						startX + axisX * (brickWidth + BRICK_SPACING + COMPENSATORY_MARGIN),
						BRICK_Y_OFFSET + (axisY * (BRICK_HEIGHT + BRICK_SPACING + COMPENSATORY_MARGIN)), 
						BRICK_WIDTH, 
						BRICK_HEIGHT
					);
				
				brick.setFilled(true);
				brick.setColor(ROW_COLOR[axisY % 10]);	
				BRICKS[index++] = brick;
				
				add(brick);
			}
		}
	}
	
	/**
	 * @param double axisX
	 * @param double axisY
	 * @return void
	 * paddle creation
	 */
	private void createPaddle(double axisX, double axisY) {
		paddle = new GRect(axisX, axisY, PADDLE_WIDTH, PADDLE_HEIGHT);
		setProperty(paddle, Color.BLACK, true);
		add(paddle);
	}

	/**
	 * @param double axisX
	 * @param double axisY
	 * @return void
	 * ball creation
	 */
	private void createBall(double axisX, double axisY) {
		ball = new GOval(axisX, axisY, BALL_DIAMETER, BALL_DIAMETER);
		ball.setFilled(true);
		ball.setFillColor(Color.BLACK);
		add(ball);
	}

	/**
	 * @param String message
	 * @return void
	 * Notifying the user about the status of game play events
	 */
	private void statusNotification(String message) {
		statusLabelNotification = createGLabelObject(
					message,
					new Font("Monospaced", Font.PLAIN, (int) fontSize),
					ORANGE_COLOR,
					halfWidth,
					halfHeight
				);

		add(statusLabelNotification);
	}

	/**
	 * @return void
	 * displays up-to-date information about the state of the game play
	 */
	private void createScoreLabel() {
		scoreLabel = new GLabel("SCORE: " + score);
		setPropertyForLabel(
				scoreLabel, 
				new Font("Monospaced", Font.PLAIN, (int) fontSize), 
				ORANGE_COLOR
			);
		
		double width = scoreLabel.getWidth();
		scoreLabel.setLocation(screenWidth - (width + LABEL_OFFSET), LABEL_OFFSET);
		
		add(scoreLabel);
	}
	
	/**
	 * @return void
	 * displays up-to-date information about the state of the game play
	 */
	private void createLivesLabel() {
		livesLabel = new GLabel("LIVES: " + lives);
		setPropertyForLabel(
				livesLabel, 
				new Font("Monospaced", Font.PLAIN, (int) fontSize), 
				ORANGE_COLOR
			);
	
		livesLabel.setLocation(LABEL_OFFSET, LABEL_OFFSET);

		add(livesLabel);
	}
	
	/* DYNAMIC EVENT HANDLERS */
	
	/**
	 * @return boolean
	 * Determining ball movement within the user screen. 
	 * Calculating the position of the ball within the screen 
	 * and determining the loss of the ball when it goes 
	 * beyond the lower boundary of the screen itself
	 */
	private boolean isBallNoCollisions() {
		GPoint ballLocation = ball.getLocation();
	
		boolean isBallCollidedX = (ballLocation.getX() <= 0) || 
								  (ballLocation.getX() + BALL_DIAMETER > screenWidth);
		
		vx = isBallCollidedX ? -vx : vx;
		vy = (ball.getY() <= 0) ? -vy : vy;

		return (ball.getY() > screenHeight);
	}

	/**
	 * @return void
	 * method calculates the bounce angle of the ball and sets the velocity of the ball
	 */
	private void reboundBall() {
		double maxSpeed = MAX_SPEED_BALL;
		GPoint ballLocation = ball.getLocation();
		
		/* let's start destroy! */
		isRebound = true;
		
		int ballPositionY = (int) (paddle.getY() - ball.getHeight());
		ball.setLocation(ballLocation.getX(), ballPositionY);
		
		double speed = Math.sqrt(vx * vx + vy * vy);
		if (speed > maxSpeed) {
			vx = vx / speed * maxSpeed;
			vy = vy / speed * maxSpeed;
		}
		vx *= REBOUND_RATIO;
		vy = -vy * REBOUND_RATIO;
	}
	
	/**
	 * 
	 * @param GOval ball
	 * @return GOval or null
	 * Determine the points of a sphere that is essentially a square on the coordinate axis.
	 * Determine the 4 points of the object boundary and determine the object
	 * caught in the intersection plane as it moves with it 
	 */
	private GObject getCollidingObject(GOval ball) {
		GPoint ballLocation = ball.getLocation();
		GPoint point[] = {
				new GPoint(0, 0), 
				new GPoint(BALL_DIAMETER, 0), 
				new GPoint(0, BALL_DIAMETER), 
				new GPoint(BALL_DIAMETER, BALL_DIAMETER)
			};
		
		for (int i = 0; i < point.length; i++) {
			GObject collider = getElementAt(
					ballLocation.getX() + point[i].getX(), 
					ballLocation.getY() + point[i].getY()
				);
			 
			if (collider != null) return collider;
		}	
		return null;
	}
	
	/**
	 * 
	 * @param GRect object "brick"
	 * @return void
	 * the method handles the place where the ball collides with the brick
	 * and removes the item from the screen 
	 */
	private void destroyBrick(GObject brick) {
    GPoint ballLocation = ball.getLocation();
	    
	    boolean isCollisionX = (ballLocation.getX() + BALL_RADIUS <= brick.getX()) ||
	    					   (ballLocation.getX() >= (brick.getX() + brick.getWidth()));

	    vy = isCollisionX ? vy : -vy;
	    vx = isCollisionX ? -vx : vx;
	    
	 					    
	    updateState();
	    remove(brick);
	}
	
	
	/* SIRVICE FUNCTION */
	
	/**
	 * @return void
	 * removing items from the user screen 
	 */
	private void destroyViewElement() {
		remove(statusLabelNotification);
		remove(ball);
		remove(paddle);
	}
	
	/**
	 * @param bricks
	 * @return void
	 * clearing the bricks collection for a new recalculation and start of the game 
	 */
	private void removeBricks(GRect[] bricks) {
		for (int i = 0; i < bricks.length; i++) {
			remove(bricks[i]);
		}
	}
		
	/**
	 * @param GRect object
	 * @param Color color
	 * @param boolean isFilled
	 * @return void
	 * set properties for an GRect object 
	 */
	private void setProperty(GRect object, Color color, boolean isFilled) {
		object.setFilled(isFilled);
		object.setColor(color);
	}
	
	/**
	 * @param GLabel label
	 * @param Font font
	 * @param Color color
	 * @return void
	 * set properties for an GLabel object 
	 */
	private void setPropertyForLabel(GLabel label, Font font, Color color) {
		label.setFont(font);
		label.setColor(color);
	}
		
	/**
	 * 
	 * @param String text
	 * @param Font font
	 * @param Font color
	 * @param double x
	 * @param double y
	 * @return GLabel
	 * constructor method that creates the object, sets the properties and returns its
	 * function for reuse in the class
	 */
	private GLabel createGLabelObject(String text, Font font, Color color, double x, double y) {
		GLabel label = new GLabel(text);
		label.setFont(font);
		label.setColor(color);
		double width = label.getWidth();
		double height = label.getDescent();
		label.setLocation(x - width / 2, y - height / 2);
	
		return label;
	}
	
	/**
	 * @return void
	 * method specifies the speed and randomness of the ball on the X-axis
	 */
	private void setInitialRandomBallMovement(double minSpeed, double maxSpeed) {
		RandomGenerator rgen = RandomGenerator.getInstance();
		vx = rgen.nextDouble(minSpeed, maxSpeed);
		if (rgen.nextBoolean(0.5)) {
			vx = -vx;
		}
		vy = maxSpeed;
	}

	/* UPDATE STATE */
	
	/**
	 * @return void
	 * updates the status of the player's achievements and remaining bricks in the game
	 */
	private void updateState() {
	    updateScoreLabel(score += 1);
	    bricksCounter--;
	}
	
	/**
	 * @param value, type integer, user achievements!
	 * @return void
	 * method redraws the updated value in the user window 
	 */
	private void updateScoreLabel(int value) {
		scoreLabel.setLabel("SCORE: " + value);
	}

	/**
	 * @param value, type integer, the number of attempts in the game by the user 
	 * @return void
	 * method redraws the updated value in the user window 
	 */
	private void updateLivesLabel(int value) {
		livesLabel.setLabel("LIVES: " + value);
	}

	/* LISTENERS */
	
	/**
	 * @param MouseEvent event
	 * @return void
	 * shows and hides game play states to the user
	 */
	public void mouseClicked(MouseEvent event) {
		statusLabelNotification.setVisible(false);
	}
	
	/**
	 * @param MouseEvent event
	 * @return void
	 * calculation of the petal position relative to the position of the mouse cursor on the screen
	 */
	public void mouseMoved(MouseEvent event) {
		int mouseX = event.getX();
		double paddleWidth = paddle.getWidth();
		double position = Math.max(
				Math.min(mouseX - paddleWidth / 2, screenWidth - paddleWidth), 
				0
			);
		
		paddle.setLocation((int) position, screenHeight - PADDLE_WIDTH - PADDLE_Y_OFFSET);
	}
}
