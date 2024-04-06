package org.example;

import org.lwjgl.opengl.*;

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

    private static final int BORDER_OFFSET = BORDER_SIZE / (WINDOW_WIDTH / GRID_SIZE); // Border offset in grid cells

    private Snake snake;
    private Point food;
    private boolean gameOver;
    private long window;
    private int score;

    private long vg; // The NanoVG context handle

    public SnakeGame() {
        start();
    }

    private void start() {
        // Initialize the snake in the middle of the screen
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
                    case GLFW_KEY_R -> restart();
                    case GLFW_KEY_Q -> glfwSetWindowShouldClose(window, true);
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
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastUpdateTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer


            if (deltaTime >= 0.1) { // 10 updates per second
                update();
                lastUpdateTime = currentTime;
            }

            if (gameOver) {
                renderGameOver();
                while (gameOver && !glfwWindowShouldClose(window)) {
                    glfwPollEvents(); // Keep the window responsive
                }
            }

            render();
            glfwSwapBuffers(window); // Swap the color buffers
            glfwPollEvents();
        }
    }

    private void update() {
        if (gameOver) {
            renderGameOver();
            return;
        }

        // Point head = snake.getHead();
        Point newHead = snake.getNewHead();
        System.out.println("Current position - X: " + newHead.x + ", Y: " + newHead.y);

        // Check if the new head position is out of the play area
        if (newHead.x < BORDER_OFFSET || newHead.x >= GRID_SIZE - BORDER_OFFSET ||
                newHead.y < BORDER_OFFSET || newHead.y >= GRID_SIZE - BORDER_OFFSET) {
            gameOver = true;
            return;
        }

        // Collision with itself
        if (snake.body.contains(newHead)) {
            gameOver = true;
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

    private void render() {
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

        // Set the style for the "GAME OVER" text
        nvgFontSize(vg, 48.0f); // Font size
        nvgFontFace(vg, "Poppins"); // Font family
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE); // Centered text

        // Calculate the position to draw the text. This example centers it on the screen.
        float x = WINDOW_WIDTH / 2.0f; // Halfway across the width of the window
        float y = WINDOW_HEIGHT / 2.0f; // Halfway down the height of the window
        float lineHeight = 48.0f;

        // Draw the "GAME OVER" text
        nvgText(vg, x, y, "GAME OVER");

        // Move down by lineHeight to render the next line of text
        y += lineHeight;

        // Render the score on the next line
        nvgText(vg, x, y, "Your score: " + score);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
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
        gameOver = false;
        start();
    }

    private void cleanup() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW
        glfwTerminate();
    }

    public static void main(String[] args) {
        new SnakeGame().run();
    }
}
