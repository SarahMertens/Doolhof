package com.sarah.doolhof.presentation.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class Particles {

    private static final int LIMIT = 600;

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double age;
        double lifetime;
        double size;
        Color color;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    void burst(double x, double y, int count, double speed, double lifetime, double size, Color color) {
        for (int i = 0; i < count && particles.size() < LIMIT; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double velocity = speed * (0.3 + random.nextDouble() * 0.7);

            Particle particle = new Particle();
            particle.x = x;
            particle.y = y;
            particle.vx = Math.cos(angle) * velocity;
            particle.vy = Math.sin(angle) * velocity;
            particle.lifetime = lifetime * (0.6 + random.nextDouble() * 0.4);
            particle.size = size * (0.6 + random.nextDouble() * 0.8);
            particle.color = color;
            particles.add(particle);
        }
    }

    void update(double dt) {
        for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext(); ) {
            Particle particle = iterator.next();
            particle.age += dt;
            if (particle.age >= particle.lifetime) {
                iterator.remove();
                continue;
            }

            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;

            // Slow down, like dust in air.
            double drag = Math.exp(-3 * dt);
            particle.vx *= drag;
            particle.vy *= drag;
        }
    }

    void draw(Graphics2D g, double left, double top, double tile) {
        for (Particle particle : particles) {
            double remaining = 1 - particle.age / particle.lifetime;
            double size = particle.size * tile * (0.4 + 0.6 * remaining);

            g.setColor(Palette.withAlpha(particle.color, remaining));
            g.fill(new Ellipse2D.Double(
                (particle.x - left) * tile - size / 2,
                (particle.y - top) * tile - size / 2,
                size, size));
        }
    }

    void clear() {
        particles.clear();
    }
}
