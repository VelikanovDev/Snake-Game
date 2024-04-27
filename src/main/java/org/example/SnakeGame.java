package org.example;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
    private static final int GRID_SIZE = 30; // The size of the grid for the snake game

    private static final int BORDER_OFFSET =
            BORDER_SIZE / (WINDOW_WIDTH / GRID_SIZE); // Border offset in grid cells

    // Updates per second
    private static final double EASY = 0.1;
    private static final double MEDIUM = 0.07;
    private static final double HARD = 0.05;

    private Snake snake;
    private Point food;
    private long window;
    private int score;

    private long vg; // The NanoVG context handle

    private List<Button> buttons;
    private GameState gameState;
    private double difficulty; // EASY, MEDIUM, HARD

    private int foodTexture;

    public SnakeGame() {
        mainMenu();
    }

    private void mainMenu() {
        gameState = GameState.MAIN_MENU;
        buttons = new ArrayList<>();
    }

    private void help() {
        gameState = GameState.HELP; // Set the game state to HELP
    }

    private void difficultyMenu() {
        gameState = GameState.DIFFICULTY_MENU;
    }

    private void play(double difficultyValue) {
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

        // Set window to be non-resizable
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Snake Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
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
                    case GLFW_KEY_R ->  {
                        if(gameState == GameState.PLAYING || gameState == GameState.GAME_OVER) {
                            restart();
                        }
                    }
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

        foodTexture = TextureLoader.loadTexture("/textures/apple.png");

        // Initialize NanoVG
        initNanoVG();
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        glClearColor(0.0f, 0.4f, 0.78f, 0.0f);

        // Initialize the last update time variable
        double lastUpdateTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastUpdateTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            switch (gameState) {
                case PLAYING -> {
                    renderGame();

                    if (deltaTime >= difficulty) {
                        update();
                        lastUpdateTime = currentTime;
                    }
                }

                case MAIN_MENU -> {
                    renderMainMenu();
                    while (gameState == GameState.MAIN_MENU && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }

                case HELP -> {
                    renderHelpScreen();
                    while (gameState == GameState.HELP && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }
                case DIFFICULTY_MENU -> {
                    renderDifficultyMenu();
                    while (gameState == GameState.DIFFICULTY_MENU && !glfwWindowShouldClose(window)) {
                        glfwPollEvents(); // Keep the window responsive
                    }
                }

                case GAME_OVER -> {
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
        // Render the difficulty and score
        renderDifficultyAndScore();
        nvgEndFrame(vg);
        renderPlayAreaBorder();
        renderSnake();
        renderFood();
    }

    private void renderSnake() {
        glColor3f(0.1f, 0.7f, 0.1f); // Soft green
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
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, foodTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Ensure full color and alpha

        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(food.x, food.y);
        glTexCoord2f(1.0f, 0.0f); glVertex2f(food.x + 1, food.y);
        glTexCoord2f(1.0f, 1.0f); glVertex2f(food.x + 1, food.y + 1);
        glTexCoord2f(0.0f, 1.0f); glVertex2f(food.x, food.y + 1);
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    private void renderPlayAreaBorder() {
        glColor3f(0.75f, 0.75f, 0.75f); // Light grey
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
        renderCenteredText("GAME OVER", y - lineHeight * 2, 48, color);

        // Draw score text
        renderCenteredText("Your score: " + score, y - lineHeight, 24, color);

        buttons.clear(); // Clear previous buttons

        // Render button during game over
        Button restartButton = new Button(x - 50, y, 100, 50, "Restart", this::restart);
        buttons.add(restartButton);
        renderButton(restartButton, color);

        Button mainMenuButton = new Button(x - 50, y + lineHeight, 100, 50, "Main menu", this::mainMenu);
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
        nvgRGBA((byte)25.5, (byte)178.5, (byte)25.5, (byte)255, color); // Light gray background

        nvgFillColor(vg, color);
        nvgFill(vg);

        // Set text color
        nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, color); // Black text
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

        // Load font from classpath resource
        String fontPath = "/fonts/Poppins-Regular.ttf"; // Path should be relative to the classpath
        InputStream fontInputStream = SnakeGame.class.getResourceAsStream(fontPath);
        if (fontInputStream == null) {
            throw new RuntimeException("Font file not found: " + fontPath);
        }

        try {
            // Create a temporary file to copy the font
            File tempFile = File.createTempFile("Poppins-Regular", ".ttf");
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile);
                 ReadableByteChannel rbc = Channels.newChannel(fontInputStream)) {
                out.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            // Load the font from the temporary file path
            int font = nvgCreateFont(vg, "Poppins", tempFile.getAbsolutePath());
            if (font == -1) {
                throw new RuntimeException("Failed to create font from: " + tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font resource", e);
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

    private void renderDifficultyAndScore() {
        nvgFontSize(vg, 36.0f);
        nvgFontFace(vg, "Poppins");
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

        // Calculate positions
        float scoreX = WINDOW_WIDTH / 2.0f;  // Center of the window
        float scoreY = BORDER_OFFSET + (float) BORDER_SIZE / 2; // Near the top of the window
        float difficultyY = WINDOW_HEIGHT - (BORDER_OFFSET + (float) BORDER_SIZE / 2); // Near the bottom of the window

        NVGColor textColor = NVGColor.create();
        nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, textColor); // White color
        nvgFillColor(vg, textColor);

        // Draw Score
        String scoreText = "Score: " + score;
        nvgText(vg, scoreX, scoreY, scoreText);

        // Draw Difficulty
        String difficultyText = "Difficulty: " + getDifficultyName();
        nvgText(vg, scoreX, difficultyY, difficultyText);
    }

    private String getDifficultyName() {
        if (difficulty == EASY) {
            return "EASY";
        } else if (difficulty == MEDIUM) {
            return "MEDIUM";
        } else if (difficulty == HARD) {
            return "HARD";
        }
        return ""; // Default case, should not happen
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
        float lineHeight = 68.0f;

        // Draw "Snake Game" text
        renderCenteredText("Snake Game", y - lineHeight * 2, 48, color);

        buttons.clear(); // Clear previous buttons

        Button playButton = new Button(x - 50, y - lineHeight, 100, 50, "Play", this::difficultyMenu);
        buttons.add(playButton);
        renderButton(playButton, color);

        Button helpButton = new Button(x - 50, y, 100, 50, "Help", this::help);
        buttons.add(helpButton);
        renderButton(helpButton, color);

        Button quitButton = new Button(x - 50, y + lineHeight, 100, 50, "Quit", () -> System.exit(0));
        buttons.add(quitButton);
        renderButton(quitButton, color);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
    }

    private void renderHelpScreen() {
        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1);

        NVGColor color = NVGColor.create(); // Prepare color object

        float x = WINDOW_WIDTH / 2.0f; // Halfway across the width of the window
        float y = WINDOW_HEIGHT / 2.0f; // Halfway down the height of the window
        float lineHeight = 68.0f;

        // Instructions
        String text1 = "Use the arrow keys to navigate the snake towards the food.";
        String text2 = "Avoid the walls and your own tail.";
        String text3 = "Press 'R' to restart at any time.";
        String text4 = "Press 'Q' to quit the game";

        renderCenteredText(text1, y - lineHeight * 2, 36, color);
        renderCenteredText(text2, y - lineHeight, 36, color);
        renderCenteredText(text3, y, 36, color);
        renderCenteredText(text4, y + lineHeight, 36, color);

        buttons.clear(); // Clear previous buttons

        Button mainMenuButton = new Button(x - 50, y + lineHeight * 2, 100, 50, "Main menu", this::mainMenu);
        buttons.add(mainMenuButton);
        renderButton(mainMenuButton, color);

        nvgEndFrame(vg);

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

        buttons.clear(); // Clear previous buttons

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
