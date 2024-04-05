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
    private Snake snake;
    private Point food;
    private boolean gameOver;
    private long window;
    private int score;

    private long vg; // The NanoVG context handle
    private int font; // The font handle

    public SnakeGame() {
        start();
    }

    private void start() {
        // Initialize the snake in the middle of the screen
        snake = new Snake(5, 5);
        snake.direction = UP;
        score = 0;
        spawnFood();
    }

    private void spawnFood() {
        int maxX = 15;
        int maxY = 15;
        Point potentialFood;
        do {
            potentialFood = new Point((int) (Math.random() * maxX), (int) (Math.random() * maxY));
        } while (snake.body.contains(potentialFood)); // Keep generating points until it's not on the snake
        food = potentialFood; // Place the food at the free spot
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
        window = glfwCreateWindow(1000, 1000, "Snake Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_UP -> snake.direction = Direction.UP;
                    case GLFW_KEY_DOWN -> snake.direction = Direction.DOWN;
                    case GLFW_KEY_LEFT -> snake.direction = Direction.LEFT;
                    case GLFW_KEY_RIGHT -> snake.direction = Direction.RIGHT;
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


            // Update 10 times per second
            double nsPerUpdate = 1.0 / 10;
            if (deltaTime >= nsPerUpdate) {
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

        // Collision with walls
        if (newHead.x < 0 || newHead.y < 0 || newHead.x >= 15 || newHead.y >= 15) {
            System.out.println("Collision with walls - X: " + newHead.x + ", Y: " + newHead.y);
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
        // Clear the frame buffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Set up orthographic projection to match grid size
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0f, 15.0f, 15.0f, 0.0f, -1.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        nvgBeginFrame(vg, 1000, 1000, 1);

        // Set font size, face, alignment, and draw the text
        nvgFontSize(vg, 24.0f);
        nvgFontFace(vg, "Poppins");
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgText(vg, 10, 10, "Score: " + score);

        // End frame for NanoVG
        nvgEndFrame(vg);

        // Render the snake
        glColor3f(0.0f, 1.0f, 0.0f); // Green color
        for (Point segment : snake.body) {
            glBegin(GL_QUADS);
            glVertex2f(segment.x, segment.y);
            glVertex2f(segment.x + 1, segment.y);
            glVertex2f(segment.x + 1, segment.y + 1);
            glVertex2f(segment.x, segment.y + 1);
            glEnd();
        }

        // Render the food
        glColor3f(1.0f, 0.0f, 0.0f); // Red color
        glBegin(GL_QUADS);
        glVertex2f(food.x, food.y);
        glVertex2f(food.x + 1, food.y);
        glVertex2f(food.x + 1, food.y + 1);
        glVertex2f(food.x, food.y + 1);
        glEnd();
    }

    private void renderGameOver() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the screen

        nvgBeginFrame(vg, 1000, 1000, 1); // Start a new frame for NanoVG

        // Set the style for the "GAME OVER" text
        nvgFontSize(vg, 48.0f); // Font size
        nvgFontFace(vg, "Poppins"); // Font family
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE); // Centered text

        // Calculate the position to draw the text. This example centers it on the screen.
        float x = 1000 / 2.0f; // Halfway across the width of the window
        float y = 1000 / 2.0f; // Halfway down the height of the window
        float lineHeight = 48.0f;

        // Draw the "GAME OVER" text
        nvgText(vg, x, y, "GAME OVER");

        // Move down by lineHeight to render the next line of text
        y += lineHeight;

        // Render the score on the next line
        nvgText(vg, x, y, "Score: " + score);

        nvgEndFrame(vg); // End the frame

        glfwSwapBuffers(window);
    }

    private void initNanoVG() {
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new RuntimeException("Could not init NanoVG.");
        }

        // Assuming you have a .ttf font file in the resources/fonts folder
        font = nvgCreateFont(vg, "Poppins", "src/main/resources/fonts/Poppins-Regular.ttf");
        if (font == -1) {
            throw new RuntimeException("Failed to create font.");
        }
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
