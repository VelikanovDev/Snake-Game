package org.example;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.List;

import static org.example.Direction.*;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

public class SnakeGame {
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 900;
    private static final int BORDER_SIZE = 60; // Pixels for border width
    private static final int GRID_SIZE = 30; // the size of the grid for the snake game

    private static final int BORDER_OFFSET =
            BORDER_SIZE / (WINDOW_WIDTH / GRID_SIZE); // Border offset in grid cells

    // Updates per second
    private final double EASY = 0.1;
    private final double MEDIUM = 0.07;
    private final double HARD = 0.05;

    private Snake snake;
    private Point food;
    private long window;
    private int score;

    private long vg; // The NanoVG context handle

    private List<Button> buttons;
    private GameState gameState;
    private double difficulty; // EASY, MEDIUM, HARD

    public SnakeGame() {
        mainMenu();
    }

    private void mainMenu() {
        gameState = GameState.MAIN_MENU;
        buttons = new ArrayList<>();
    }

    private void difficultyMenu() {
        gameState = GameState.DIFFICULTY_MENU;
    }

    private void play(double difficultyValue) {
        System.out.println("play()");
        // Initialize the snake in the middle of the screen
        difficulty = difficultyValue;
        gameState = GameState.PLAYING;
        snake = new Snake(GRID_SIZE / 2, GRID_SIZE / 2);
        snake.direction = UP;
        score = 0;
        spawnFood();
    }

    private void spawnFood() {
        int minX = BORDER_OFFSET;
        int maxX = GRID_SIZE - BORDER_OFFSET - 1;
        int minY = BORDER_OFFSET; // Starts food spawning at the top border
        int maxY = GRID_SIZE - BORDER_OFFSET - 1;

        Point potentialFood;
        do {
            potentialFood = new Point(minX + (int) (Math.random() * (maxX - minX + 1)),
                    minY + (int) (Math.random() * (maxY - minY + 1)));
        } while (snake.body.contains(potentialFood));
        food = potentialFood;
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Snake Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback
        glfwSetKeyCallback(window, (_, key, scancode, action, _) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_UP -> {
                        if (snake.direction != Direction.DOWN) snake.direction = Direction.UP;
                    }
                    case GLFW_KEY_DOWN -> {
                        if (snake.direction != Direction.UP) snake.direction = DOWN;
                    }
                    case GLFW_KEY_LEFT -> {
                        if (snake.direction != RIGHT) snake.direction = LEFT;
                    }
                    case GLFW_KEY_RIGHT -> {
                        if (snake.direction != LEFT) snake.direction = RIGHT;
                    }
                    case GLFW_KEY_R -> restart();
                    case GLFW_KEY_Q -> System.exit(0);
                }
            }
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                double[] mouseX = new double[1];
                double[] mouseY = new double[1];
                glfwGetCursorPos(window, mouseX, mouseY);
                for (Button b : buttons) {
                    if (!(gameState == GameState.PLAYING) && b.isMouseOver((int) mouseX[0], (int) mouseY[0])) {
                        b.action.run();
                    }

                }
            }
        });

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        GL.createCapabilities();
        // Initialize NanoVG
        initNanoVG();
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Initialize the last update time variable
        double lastUpdateTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            System.out.println("Loop");
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastUpdateTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            switch (gameState) {

                case PLAYING -> {
                    System.out.println("GameState.PLAYING");
                    renderGame();

                    if (deltaTime >= difficulty) {
                        update();
                        lastUpdateTime = currentTime;
                    }

                }

                case MAIN_MENU -> {
                    System.out.println("GameState.MAIN_MENU");
                    renderMainMenu();
                    while (gameState == GameState.MAIN_MENU && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }
                case DIFFICULTY_MENU -> {
                    System.out.println("GameState.DIFFICULTY_MENU");
                    renderDifficultyMenu();
                    while (gameState == GameState.DIFFICULTY_MENU && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }

                case GAME_OVER -> {
                    System.out.println("GameState.GAME_OVER");
                    renderGameOver();
                    while (gameState == GameState.GAME_OVER && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }
            }

            glfwSwapBuffers(window); // Swap the color buffers
            glfwPollEvents();
        }
    }

    private void update() {
        Point newHead = snake.getNewHead();
        System.out.println("Current position - X: " + newHead.x + ", Y: " + newHead.y);

        // Check if the new head position is out of the play area
        if (newHead.x < BORDER_OFFSET || newHead.x >= GRID_SIZE - BORDER_OFFSET ||
                newHead.y < BORDER_OFFSET || newHead.y >= GRID_SIZE - BORDER_OFFSET) {
            gameState = GameState.GAME_OVER;
            return;
        }

        // Collision with itself
        if (snake.body.contains(newHead)) {
            gameState = GameState.GAME_OVER;
            return;
        }

        // Eating food
        if (newHead.equals(food)) {
            score++;
            snake.grow(newHead);
            spawnFood();
        } else {
            snake.move(newHead);
        }
    }

    private void renderGame() {
        setupProjection();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1);
        renderScore();
        nvgEndFrame(vg);
        renderPlayAreaBorder();
        renderSnake();
        renderFood();
    }

    private void renderSnake() {
        glColor3f(0.0f, 1.0f, 0.0f); // Green color for the snake
        for (Point segment : snake.body) {
            glBegin(GL_QUADS);
            glVertex2f(segment.x, segment.y);
            glVertex2f(segment.x + 1, segment.y);
            glVertex2f(segment.x + 1, segment.y + 1);
            glVertex2f(segment.x, segment.y + 1);
            glEnd();
        }
    }

    private void renderFood() {
        glColor3f(1.0f, 0.0f, 0.0f); // Red color
        glBegin(GL_QUADS);
        glVertex2f(food.x, food.y);
        glVertex2f(food.x + 1, food.y);
        glVertex2f(food.x + 1, food.y + 1);
        glVertex2f(food.x, food.y + 1);
        glEnd();
    }

    private void renderPlayAreaBorder() {
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(BORDER_OFFSET, BORDER_OFFSET);
        glVertex2f(GRID_SIZE - BORDER_OFFSET, BORDER_OFFSET);
        glVertex2f(GRID_SIZE - BORDER_OFFSET, GRID_SIZE - BORDER_OFFSET);
        glVertex2f(BORDER_OFFSET, GRID_SIZE - BORDER_OFFSET);
        glEnd();
    }

    private void renderGameOver() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the screen

        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1); // Start a new frame for NanoVG

        NVGColor color = NVGColor.create(); // Prepare color object

        float x = WINDOW_WIDTH / 2.0f; // Halfway across the width of the window
        float y = WINDOW_HEIGHT / 2.0f; // Halfway down the height of the window
        float lineHeight = 68.0f;

        // Draw "GAME OVER" text
        renderCenteredText("GAME OVER", y - lineHeight, 48, color);

        // Draw score text
        renderCenteredText("Your score: " + score, y, 24, color);

        // Render button during game over
        Button restartButton = new Button(x - 50, y + lineHeight, 100, 50, "Restart", this::restart);
        buttons.add(restartButton);
        renderButton(restartButton, color);

        Button mainMenuButton = new Button(x - 50, y + lineHeight * 2, 100, 50, "Main menu", this::mainMenu);
        buttons.add(mainMenuButton);
        renderButton(mainMenuButton, color);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
    }

    private void renderCenteredText(String text, float y, int fontSize, NVGColor color) {
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "Poppins");
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, color); // White text color
        nvgFillColor(vg, color);
        nvgText(vg, WINDOW_WIDTH / 2.0f, y, text);
    }

    private void renderButton(Button button, NVGColor color) {
        nvgBeginPath(vg);
        nvgRect(vg, button.x, button.y, button.width, button.height);

        // Set background color for the button
        nvgRGBA((byte)200, (byte)200, (byte)200, (byte)255, color); // Light gray background
        nvgFillColor(vg, color);
        nvgFill(vg);

        // Set text color
        nvgRGBA((byte)0, (byte)0, (byte)0, (byte)255, color); // Black text
        nvgFillColor(vg, color);
        nvgFontSize(vg, 20);
        nvgFontFace(vg, "Poppins");
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(vg, button.x + button.width / 2.0f, button.y + button.height / 2.0f, button.label);

        nvgClosePath(vg);
    }

    private void initNanoVG() {
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new RuntimeException("Could not init NanoVG.");
        }

        // The font handle
        int font = nvgCreateFont(vg, "Poppins", "src/main/resources/fonts/Poppins-Regular.ttf");
        if (font == -1) {
            throw new RuntimeException("Failed to create font.");
        }
    }

    private void setupProjection() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        // Adjusting the projection to center the grid in the window
        glOrtho(0.0f, GRID_SIZE, GRID_SIZE, 0.0f, -1.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    private void renderScore() {
        nvgFontSize(vg, 36.0f);
        nvgFontFace(vg, "Poppins");
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(vg, WINDOW_WIDTH / 2.0f, BORDER_OFFSET + ((float) BORDER_SIZE / 2), "Score: " + score);
    }

    private void restart() {
        gameState = GameState.MAIN_MENU;
        play(difficulty);
    }

    private void cleanup() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW
        glfwTerminate();
    }

    private void renderMainMenu() {
        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1); // Start a new frame for NanoVG

        NVGColor color = NVGColor.create(); // Prepare color object

        float x = WINDOW_WIDTH / 2.0f; // Halfway across the width of the window
        float y = WINDOW_HEIGHT / 2.0f; // Halfway down the height of the window
        float lineHeight = 48.0f;

        // Draw "Snake Game" text
        renderCenteredText("Snake Game", y - lineHeight, 48, color);

        Button playButton = new Button(x - 50, y, 100, 50, "Play", this::difficultyMenu);
        buttons.add(playButton);
        renderButton(playButton, color);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
    }

    private void renderDifficultyMenu() {
        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1); // Start a new frame for NanoVG

        NVGColor color = NVGColor.create(); // Prepare color object

        float x = WINDOW_WIDTH / 2.0f; // Halfway across the width of the window
        float y = WINDOW_HEIGHT / 2.0f; // Halfway down the height of the window
        float lineHeight = 68.0f;

        // Draw "Snake Game" text
        renderCenteredText("Choose difficulty", y - lineHeight * 2, 48, color);

        Button easyButton = new Button(x - 50, y - lineHeight, 100, 50, "Easy", () -> play(EASY));
        buttons.add(easyButton);
        renderButton(easyButton, color);


        Button mediumButton = new Button(x - 50, y, 100, 50, "Medium", () -> play(MEDIUM));
        buttons.add(mediumButton);
        renderButton(mediumButton, color);

        Button hardButton = new Button(x - 50, y + lineHeight, 100, 50, "Hard", () -> play(HARD));
        buttons.add(hardButton);
        renderButton(hardButton, color);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
    }

    public static void main(String[] args) {
        new SnakeGame().run();
    }
}
