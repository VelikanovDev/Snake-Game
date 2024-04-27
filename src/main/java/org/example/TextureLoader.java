package org.example;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureLoader {
    /**
     * Loads an image file as an OpenGL texture from the classpath.
     * @param resourcePath The classpath to the texture image.
     * @return The OpenGL texture ID for the loaded texture.
     */
    public static int loadTexture(String resourcePath) {
        ByteBuffer image; // Buffer to hold image data
        int width, height; // Width and height of the image
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer comp = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            // Load image data using STB Image and classpath resource stream
            image = loadImageResource(resourcePath, w, h, comp);
            if (image == null) {
                throw new RuntimeException("Failed to load a texture file! " +
                        "Unable to open file at " + resourcePath);
            }

            width = w.get();
            height = h.get();
        }

        // Generate a new OpenGL texture ID
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID); // Bind this texture ID as a 2D texture

        // Set texture wrapping and filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Upload the image data to the texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

        return textureID;
    }

    private static ByteBuffer loadImageResource(String resourcePath, IntBuffer w, IntBuffer h, IntBuffer comp) {
        ByteBuffer imageBuffer;
        // Try with resources to ensure proper closure of stream
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            try (ReadableByteChannel rbc = Channels.newChannel(stream)) {
                imageBuffer = BufferUtils.createByteBuffer(8 * 1024);

                while (true) {
                    int bytes = rbc.read(imageBuffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (imageBuffer.remaining() == 0) {
                        imageBuffer = resizeBuffer(imageBuffer, imageBuffer.capacity() * 2);
                    }
                }
                imageBuffer.flip();
            }
            return STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, STBImage.STBI_rgb_alpha);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture file!", e);
        }
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
