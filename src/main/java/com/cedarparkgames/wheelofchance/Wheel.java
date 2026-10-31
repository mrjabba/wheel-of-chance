package com.cedarparkgames.wheelofchance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Wheel values and landing math, no libGDX. Slice i spans [i*w, (i+1)*w] degrees from the wheel's rotation; the pointer sits at 90°. */
public class Wheel {
    public static final int SLICES = 12;
    public final int[] values = new int[SLICES];
    private final List<Integer> rigged;
    private final Random rnd;

    public Wheel(Random rnd, List<Integer> rigged) {
        if (rigged.size() > SLICES) throw new IllegalArgumentException("at most " + SLICES + " predetermined values");
        this.rnd = rnd;
        this.rigged = rigged;
        for (int i = 0; i < SLICES; i++) values[i] = rnd.nextInt(101);
        // make sure every rigged value is actually on the wheel, spread around it
        for (int k = 0; k < rigged.size(); k++) values[k * SLICES / rigged.size()] = rigged.get(k);
    }

    public static float sliceDegrees() {
        return 360f / SLICES;
    }

    public int pickTarget() {
        if (rigged.isEmpty()) return rnd.nextInt(SLICES);
        int v = rigged.get(rnd.nextInt(rigged.size()));
        for (int i = 0; i < SLICES; i++) if (values[i] == v) return i;
        throw new IllegalStateException("unreachable");
    }

    /** Index of the slice under the pointer at this rotation. */
    public int sliceAt(float rotation) {
        float a = ((90 - rotation) % 360 + 360) % 360;
        return (int) (a / sliceDegrees()) % SLICES;
    }

    /** Final rotation that lands slice `index` under the pointer after several full turns. */
    public float targetRotation(float current, int index) {
        float w = sliceDegrees();
        float jitter = (rnd.nextFloat() - 0.5f) * 0.7f * w; // stay well inside the slice
        float want = 90 - (index + 0.5f) * w + jitter;
        float delta = ((want - current) % 360 + 360) % 360;
        return current + delta + 360 * (5 + rnd.nextInt(3));
    }

    /** "10, 70" -> [10, 70]; null/blank -> fair wheel. */
    public static List<Integer> parse(String s) {
        List<Integer> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        for (String part : s.split(",")) {
            if (part.isBlank()) continue;
            int v = Integer.parseInt(part.trim());
            if (v < 0 || v > 100) throw new IllegalArgumentException("predetermined value out of 0..100: " + v);
            out.add(v);
        }
        return out;
    }
}
