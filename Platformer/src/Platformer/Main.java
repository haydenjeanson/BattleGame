package Platformer;

import org.lwjgl.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

import entities.AbstractEntity;
import entities.AbstractMoveableEntity;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 480;
	private boolean isRunning = true;
	private Ground[] ground = new Ground[2];

	private Box[] box = new Box[2];
	private int[] playerHealth = new int[box.length];
	private boolean[] isDead = new boolean[box.length];

	private static final double PLAYER_SPEED = 0.2;
	private static final double BULLET_SPEED = 0.6;
	private static final int SHOT_SPEED = 500; // Higher = slower
	private static final int ENEMY_HEALTH = 5;
	private static final double GRAVITY = 0.35;
	private static final double GRAVITY_MULTIPLIER = 1.5;	
	// The two jump finals combined make up the jump height
	private static final int JUMP_HEIGHT = 100;
	private static final double JUMP_SPEED = 0.8;

	private List<Bullet> bulletsLeft = new ArrayList<Bullet>();
	private List<Bullet> bulletsRight = new ArrayList<Bullet>();
	private List<Bullet> bulletsToRemove = new ArrayList<Bullet>();

	//	private List<Enemy> enemies = new ArrayList<Enemy>();
	//	private List<Enemy> enemiesToRemove = new ArrayList<Enemy>();
	//	private List<Integer> enemyHealth = new ArrayList<Integer>();

	private boolean[] isJumping = new boolean[box.length];  // Add new value in jump method for each of these
	private boolean[] isFalling = new boolean[box.length];  // Add new value in gravity method for each of these
	private	double[] gravitySpeed = new double [box.length];
	private int[] timeSinceLastJump = new int[box.length];
	private int[] secondsPassedSinceLastShot = new int[box.length]; // Add new value in shooting method for each of these
	private int secondsPassedSinceLastEnemy = 0;
	private	int[] groundNumber = new int[ground.length];
	private double[] moveSpeed = new double[box.length];

	public Main() {
		setUpDisplay();
		setUpOpenGL();
		setUpEntities();
		setUpTimer();

		while (isRunning) {
			render();
			logic(getDelta());
			
			Display.update();
			Display.sync(60);
			if (Display.isCloseRequested()) {
				isRunning = false;
			}
		}
		Display.destroy();
	}

	private long lastFrame;

	private long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	private int getDelta() {
		long currentTime = getTime();
		int delta = (int)(currentTime - lastFrame);
		lastFrame = currentTime;
		return delta;
	}

	private void setUpDisplay() { 
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.setTitle("My Platformer");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	private void setUpOpenGL() { 
		//Initialization code OpenGl
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, WIDTH, HEIGHT, 0, 1, -1);
		glMatrixMode(GL_MODELVIEW);
	}

	private void setUpEntities() { 

		for (int i = 0; i < box.length; i++) {
			box[i] = new Box((WIDTH / 2 - 20 / 2) * i + 1, HEIGHT - 60, 20, 40);

			playerHealth[i] = 5;
			isDead[i] = false;

			isJumping[i] = false;
			timeSinceLastJump[i] = 0;
			isFalling[i] = false;
			gravitySpeed[i] = GRAVITY;
			secondsPassedSinceLastShot[i] = SHOT_SPEED;
			moveSpeed[i] = PLAYER_SPEED;
			groundNumber[i] = -1;
		}

		ground[0] = new Ground(0, HEIGHT - 20, WIDTH, 10);
		ground[1] = new Ground(50, ground[0].getY() - 30, 200, 10);
		//ground[2] = new Ground(350, ground[0].getY() - 30, 200, 10);
	}

	private void setUpTimer() { 
		lastFrame = getTime();
	}

	private void render() { 
		glClear(GL_COLOR_BUFFER_BIT);

		glColor3f(0.4f, 0.6f, 1.0f);
		glRectf(0, 0, WIDTH, HEIGHT);

		for (int i = 0; i < box.length; i++) {
			box[i].draw((float)(1.0 * i), 0.5f, 1.0f);
		}

		for (int i = 0; i < ground.length; i++) {
			ground[i].draw(0f, 1.0f, 0.3f);
		}

		// Draw all entities in List bullets
		for (Bullet entity : bulletsRight) {
			entity.draw(1, 1, 1);
		}
		for (Bullet entity : bulletsLeft) {
			entity.draw(1, 1, 1);
		}

		//		for (Enemy enemy : enemies) {
		//			enemy.draw(1.0f, 0, 0);
		//        }
	}
	private void logic(int delta) { 

		//		System.out.println(delta);

		for (int i = 0; i < box.length; i++) {
			if (!isDead[i]) {
				box[i].update(delta);

				LRMovement(box[i]);

				gravity(box[i]);  // Apply gravity to the box
				alignToGround(box[i]);

				jump(box[i], delta);
				crouch(box[i]);

				shooting(box[i], delta);
			}
		}

		//addEnemy(delta);

		for (Bullet entity : bulletsRight) {
			entity.setDX(BULLET_SPEED);
			entity.update(delta);

			if (entity.getX() > WIDTH) {
				bulletsToRemove.add(entity);
			} else if (entity.getX() < 0) {
				bulletsToRemove.add(entity);
			}

			for (int i = 0; i < box.length; i++) {
				if (entity.intersects(box[i])) {
					bulletsToRemove.add(entity);
					playerHealth[i]--;

					System.out.println(playerHealth[i]);

					if (playerHealth[i] == 0) {
						isDead[i] = true;
						
						box[i].setHeight(10);
						box[i].setWidth(10);
						
						box[i].setY(ground[groundNumber[i]].getY() - box[i].getHeight());
					}
				}
			}
			//			int currentEnemy = -1;
			//			for (Enemy enemy : enemies) {
			//				currentEnemy++;
			//				if (entity.intersects(enemy)) {
			//					bulletsToRemove.add(entity);
			//					enemyHealth.set(currentEnemy, enemyHealth.get(currentEnemy) - 1);
			//					System.out.println(enemyHealth.get(currentEnemy));
			//
			//					if (enemyHealth.get(currentEnemy) == 0) {
			//						enemiesToRemove.add(enemy);
			//						enemyHealth.set(currentEnemy, ENEMY_HEALTH);
			//					}
			//				}
			//			}
		}

		for (Bullet entity : bulletsLeft) {
			entity.setDX(BULLET_SPEED * - 1);
			entity.update(delta);

			if (entity.getX() > WIDTH) {
				bulletsToRemove.add(entity);
			} else if (entity.getX() < 0) {
				bulletsToRemove.add(entity);
			}

			for (int i = 0; i < box.length; i++) {
				if (entity.intersects(box[i])) {
					bulletsToRemove.add(entity);
					playerHealth[i]--;

					System.out.println(playerHealth[i]);

					if (playerHealth[i] == 0) {
						isDead[i] = true;

						box[i].setHeight(10);
						box[i].setWidth(10);
						
						box[i].setY(ground[groundNumber[i]].getY() - box[i].getHeight());
					}
				}
			}

			//			int currentEnemy = -1;
			//			for (Enemy enemy : enemies) {
			//				currentEnemy++;
			//				if (entity.intersects(enemy)) {
			//					bulletsToRemove.add(entity);
			//					enemyHealth.set(currentEnemy, enemyHealth.get(currentEnemy) - 1);
			//					System.out.println(enemyHealth.get(currentEnemy));
			//
			//					if (enemyHealth.get(currentEnemy) == 0) {
			//						enemiesToRemove.add(enemy);
			//						enemyHealth.set(currentEnemy, ENEMY_HEALTH);
			//					}
			//				}
			//			}
		}

		bulletsRight.removeAll(bulletsToRemove);
		bulletsLeft.removeAll(bulletsToRemove);
		//		enemies.removeAll(enemiesToRemove);
	}

	//	private void addEnemy(int delta) {
	//
	//		secondsPassedSinceLastEnemy += delta;
	//
	//		int rndEnemySpawn = ThreadLocalRandom.current().nextInt(30, 610 + 1);
	//		Enemy enemy = new Enemy(rndEnemySpawn, ground[0].getY() - 23, 20, 23);
	//
	//		if (secondsPassedSinceLastEnemy >= 3000 && enemies.size() < 5) {
	//			if (enemies.size() != 0) {
	//				for (Enemy entity : enemies) {
	//					if (rndEnemySpawn <= entity.getX() + 30 && rndEnemySpawn >= entity.getX() - 30) {
	//						enemy = new Enemy(rndEnemySpawn + 40, ground[0].getY() - 23, 20, 23);
	//						System.out.println("+40");
	//					} else {
	//						enemy = new Enemy(rndEnemySpawn, ground[0].getY() - 23, 20, 23);
	//						System.out.println("0");
	//					}
	//				}
	//			}
	//
	//			enemies.add(enemy);
	//
	//			int health = ENEMY_HEALTH;
	//			enemyHealth.add(health);
	//			secondsPassedSinceLastEnemy = 0;
	//		}
	//	}

	private void shooting(Box player, int delta) {

		for (int i = 0; i < secondsPassedSinceLastShot.length; i++) {
			secondsPassedSinceLastShot[i] += delta;
		}

		// Player 1
		if (Keyboard.isKeyDown(Keyboard.KEY_D) && secondsPassedSinceLastShot[0] > SHOT_SPEED && player == box[0]) {
			Bullet bullet = new Bullet(player.getX() + player.getWidth(), player.getY() + player.getWidth() / 2, 5, 5);
			bulletsRight.add(bullet);
			secondsPassedSinceLastShot[0] = 0;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_A) && secondsPassedSinceLastShot[0] > SHOT_SPEED && player == box[0]) {
			Bullet bullet = new Bullet(player.getX(), player.getY() + player.getWidth() / 2, 5, 5);
			bulletsLeft.add(bullet);
			secondsPassedSinceLastShot[0] = 0;
		}

		// Player 2
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && secondsPassedSinceLastShot[1] > SHOT_SPEED && player == box[1]) {
			Bullet bullet = new Bullet(player.getX() + player.getWidth(), player.getY() + player.getWidth() / 2, 5, 5);
			bulletsRight.add(bullet);
			secondsPassedSinceLastShot[1] = 0;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) && secondsPassedSinceLastShot[1] > SHOT_SPEED && player == box[1]) {
			Bullet bullet = new Bullet(player.getX(), player.getY() + player.getWidth() / 2, 5, 5);
			bulletsLeft.add(bullet);
			secondsPassedSinceLastShot[1] = 0;
		}
	}

	private void LRMovement(Box player) {

		for (int i = 0; i < moveSpeed.length; i++) {
			if (box[i].getX() < 0) {
				moveSpeed[i] = PLAYER_SPEED;
			} else if (box[i].getX() + box[i].getWidth() >= WIDTH) {
				moveSpeed[i] = -PLAYER_SPEED;
			}

			box[i].setDX(moveSpeed[i]);
		}

	}


	// Handles crouching, update for each player
	private void crouch(Box player) {
		if (groundNumber[0] != -1 && player == box[0]) {
			if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
				player.setHeight(20);
				player.setY(ground[groundNumber[0]].getY() - player.getHeight());
			} else if (player == box[0]) {
				player.setHeight(40);
				player.setY(ground[groundNumber[0]].getY() - player.getHeight());
			}
		}

		if (groundNumber[1] != -1 && player == box[1]) {
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && player == box[1]) {
				player.setHeight(20);
				player.setY(ground[groundNumber[1]].getY() - player.getHeight());
			} else if (player == box[1]) {
				player.setHeight(40);
				player.setY(ground[groundNumber[1]].getY() - player.getHeight());
			}
		}
	}

	private void jump(Box player, int delta) {

		//		for (int i = 0; i < timeSinceLastJump.length; i++) {
		//			timeSinceLastJump[i] += delta;
		//		}

		isOnGround(player);

		int[] gn = new int[groundNumber.length];

		for (int i = 0; i < gn.length; i++) {
			gn[i] = groundNumber[i];
		}

		if (player == box[0]) {
			timeSinceLastJump[0] += delta;

			if (!isJumping[0] && groundNumber[0] != -1) {
				if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
					isJumping[0] = true;
				}
			} else if (isJumping[0] && groundNumber[0] == gn[0]) {
				player.setDY(-JUMP_SPEED);
				if (timeSinceLastJump[0] >= JUMP_HEIGHT) {
					player.setDY(0);
					isJumping[0] = false;
					timeSinceLastJump[0] = 0;
				}
			}
		}

		if (player == box[1]) {
			timeSinceLastJump[1] += delta;
			if (!isJumping[1] && groundNumber[1] != -1) {			
				if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
					isJumping[1] = true;
				}
			} else if (isJumping[1] && groundNumber[1] == gn[1]) {
				player.setDY(-JUMP_SPEED);
				if (timeSinceLastJump[1] >= JUMP_HEIGHT) {
					player.setDY(0);
					isJumping[1] = false;
					timeSinceLastJump[1] = 0;
				}
			}
		}
	}

	private void isOnGround(Box player) {		
		if (player == box[0]) {
			for (int i = 0; i < ground.length; i++) {
				if (player.intersects(ground[i]) && player.getY() + player.getHeight() < ground[i].getY() + ground[i].getHeight() && player == box[0]) {
					groundNumber[0] = i;
					break;
				} else {
					groundNumber[0] = -1;
				}
			}
		}

		if (player == box[1]) {
			for (int i = 0; i < ground.length; i++) {
				if (player.intersects(ground[i]) && player.getY() + player.getHeight() < ground[i].getY() + ground[i].getHeight() && player == box[1]) {
					groundNumber[1] = i;
					break;
				} else {
					groundNumber[1] = -1;
				}
			}	
		}
	}

	private void alignToGround(Box player) {
				for (int i = 0; i < ground.length; i++) {
					isOnGround(player);
//					for (int j = 0; j < isJumping .length; j++) {
						if (groundNumber[0] == i && !isJumping[0] && player == box[0]) {
							player.setY(ground[i].getY() - player.getHeight());
						}
						if (groundNumber[1] == i && !isJumping[1] && player == box[1]) {
							player.setY(ground[i].getY() - player.getHeight());
						}
//					}
				}
	}

	private void gravity(Box player) {
		isOnGround(player);

		//		for (int i = 0; i < isJumping.length; i++) {
		//			if (isFalling[0]) {
		//				gravitySpeed[0] *= GRAVITY_MULTIPLIER;
		//			}
		//			
		//			if (isFalling[1]) {
		//				gravitySpeed[1] *= GRAVITY_MULTIPLIER;
		//			}
		//		}	

		if (player == box[0]) {
			if (groundNumber[0] == -1 && !isJumping[0]) {
				player.setDY(gravitySpeed[0]);
				System.out.println(gravitySpeed[0]);
				isFalling[0] = true;
			} else if (isFalling[0]) {
				player.setDY(0);
				isFalling[0] = false;
				gravitySpeed[0] = GRAVITY;
			}
		}

		if (player == box[1]) {
			if (groundNumber[1] == -1 && !isJumping[1]) {
				player.setDY(gravitySpeed[1]);
				//		System.out.println(gravitySpeed[1] + "----------------------------");
				isFalling[1] = true;
			} else if (isFalling[1]) {
				player.setDY(0);
				isFalling[1] = false;
				gravitySpeed[1] = GRAVITY;
			}
		}
	}

	private static class Box extends AbstractMoveableEntity {

		public Box(double x, double y, double width, double height) {
			super(x, y, width, height);
		}

		@Override
		public void draw(float colorR, float colorG, float colorB) {
			glColor3f(colorR, colorG, colorB);
			glRectd(x, y, x + width, y + height);
		}

	}

	private static class Bullet extends AbstractMoveableEntity {

		public Bullet(double x, double y, double width, double height) {
			super(x, y, width, height);
		}

		@Override
		public void draw(float colorR, float colorG, float colorB) {
			glColor3f(colorR, colorG, colorB);
			glRectd(x, y, x + width, y + height);
		}

	}

	private static class Enemy extends AbstractMoveableEntity {

		public Enemy(double x, double y, double width, double height) {
			super(x, y, width, height);
		}

		@Override
		public void draw(float colorR, float colorG, float colorB) {
			glColor3f(colorR, colorG, colorB);
			glRectd(x, y, x + width, y + height);
		}

	}

	private static class Ground extends AbstractEntity {

		public Ground(double x, double y, double width, double height) {
			super(x, y, width, height);
		}

		@Override
		public void draw(float colorR, float colorG, float colorB) {
			glColor3f(colorR, colorG, colorB);
			glRectd(x, y, x + width, y + height);
		}

		@Override
		public void update(int delta) {
			// Do nothing
		}

	}

	public static void main(String[] args) {
		new Main();
	}

}
