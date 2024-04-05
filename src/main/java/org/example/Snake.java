package org.example;

import java.util.LinkedList;

public class Snake {
    LinkedList<Point> body = new LinkedList<>();
    Direction direction = Direction.RIGHT; // Default direction

    public Snake(int initialX, int initialY) {
        body.add(new Point(initialX, initialY));
    }

    void grow(Point newHead) {
        // Add a new head in the direction of movement
        body.addFirst(newHead);
    }

    void move(Point newHead) {
        grow(newHead); // Add a new head
        body.removeLast(); // Remove the tail
    }

    Point getNewHead() {
        Point head = body.getFirst();
        return switch (direction) {
            case UP -> new Point(head.x, head.y - 1);
            case DOWN -> new Point(head.x, head.y + 1);
            case LEFT -> new Point(head.x - 1, head.y);
            case RIGHT -> new Point(head.x + 1, head.y);
        };
    }
}
