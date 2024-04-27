package org.example;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBInternalformatQuery2.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureLoader {
    /**
     * Loads an image file as an OpenGL texture.
     * @param path The filesystem path to the texture image.
     * @return The OpenGL texture ID for the loaded texture.
     */
    public static int loadTexture(String path) {
        ByteBuffer image; // Buffer to hold image data
        int width, height; // Width and height of the image
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer comp = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            // Load image data using STB Image
            image = STBImage.stbi_load(path, w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load a texture file!" + System.lineSeparator() +
                        STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }

        // Generate a new OpenGL texture ID
        int textureID = glGenTextures();
        // Bind this texture ID as a 2D texture
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Set texture wrapping to prevent tiling
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        // Set texture filtering to linear for both minifying and magnifying
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Upload the image data to the texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        // Free the image data buffer
        STBImage.stbi_image_free(image);

        return textureID;
    }
}
